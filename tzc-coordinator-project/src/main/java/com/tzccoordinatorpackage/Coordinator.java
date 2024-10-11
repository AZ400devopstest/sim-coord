package com.tzccoordinatorpackage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigException;

public class Coordinator {

    private static String csvFilePath;
    private static int schedulePeriod;
    
    // Separate server details for PIXI and SICK-TIC
    private static String idrisServerHost;
    private static int idrisServerPort;
    private static String pixiServerHost;
    private static int pixiServerPort;
    private static String sickTicServerHost;
    private static int sickTicServerPort;
    
    // Sockets for PIXI and SICK-TIC servers
    private static Socket idrisSocket;
    private static Socket pixiSocket;
    private static Socket sickTicSocket;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("Coordinator started");

        // Load the configuration from properties file
        String configFilePath = null;
        if (args.length > 0) {
            configFilePath = args[0];
            System.out.println("Using external configuration file: " + configFilePath);
        } else {
            System.out.println("No external configuration file provided. Using default config.properties.");
        }

        loadConfiguration(configFilePath);

        // Establish connections to IDRIS, PIXI and SICK-TIC servers
        connectToServers();

        // Process CSV and schedule tasks
        List<Record> records = null;
        try {
            records = processCSV(csvFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Create a scheduled executor to handle periodic tasks
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10); //Adjust pool size as needed

        // Schedule each record based on TRANSACTION_DATE
        for (Record record : records) {
            long transactionDate = record.getTransactionDate();

        if (transactionDate != -1) //incase of exception while reading transaction_date this is -1
        {
            // Calculate delay in seconds (assuming TRANSACTION_DATE is in milliseconds)
            long delay = transactionDate / 1000; // Adjust based on actual unit

            scheduler.schedule(() -> {
                try {
                    System.out.println("Sending JSON object: " + record.getJsonObject().toString() +" with delay: "+delay);
                    // Send JSON data to PIXI and SICK-TIC servers
                    sendDataToServer(idrisSocket, record.getJsonObject().toString(), "IDRIS");
                    sendDataToServer(pixiSocket, record.getJsonObject().toString(), "PIXI");
                    sendDataToServer(sickTicSocket, record.getJsonObject().toString(), "SICK-TIC");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, delay, TimeUnit.SECONDS); // Adjust TimeUnit based on TRANSACTION_DATE's unit
        }

        } //all records processed
        // Optionally, shutdown the scheduler after all tasks are completed
        scheduler.shutdown();
    }

/**
 * Loads configuration from the properties file.
 * If configFilePath is provided, load from the external file.
 * Otherwise, load from the packaged config.properties.
 * @param configFilePath Path to the external config.properties file.
 */
private static void loadConfiguration(String configFilePath) {
            Config config=ConfigFactory.parseResources("config.properties");

            try{
            if (configFilePath != null) {
                config = ConfigFactory.parseFile(new java.io.File(configFilePath));
                System.out.println("Using external configuration file: " + configFilePath);
            } else {
                config = ConfigFactory.load(); // Load from default application.conf
                System.out.println("No external configuration file provided. Using default config.");
            }

            // Load the configuration parameters
            csvFilePath = config.getString("csvFilePath");
            schedulePeriod = config.getInt("schedulePeriod");

            idrisServerHost = config.getString("IdrisServerHost");
            idrisServerPort = config.getInt("IdrisServerPort");

            pixiServerHost = config.getString("PixiServerHost");
            pixiServerPort = config.getInt("PixiServerPort");

            sickTicServerHost = config.getString("SickTicServerHost");
            sickTicServerPort = config.getInt("SickTicServerPort");

            // Print the loaded configuration
            System.out.println("Loaded configuration: ");
            System.out.println("CSV File Path: " + csvFilePath);
            System.out.println("Schedule Period: " + schedulePeriod + " seconds");
            System.out.println("IDRIS Server: " + idrisServerHost + " on port " + idrisServerPort);
            System.out.println("PIXI Server: " + pixiServerHost + " on port " + pixiServerPort);
            System.out.println("SICK-TIC Server: " + sickTicServerHost + " on port " + sickTicServerPort);

        } catch (ConfigException ex) {
            System.err.println("Configuration error: "+ ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Establishes connections to the PIXI and SICK-TIC servers.
     */
    private static void connectToServers() {
        try {
            // Connect to IDRIS server
            idrisSocket = new Socket(idrisServerHost, idrisServerPort);
            System.out.println("Connected to IDRIS server: " + idrisServerHost + " on port " + idrisServerPort);

            // Connect to PIXI server
            pixiSocket = new Socket(pixiServerHost, pixiServerPort);
            System.out.println("Connected to PIXI server: " + pixiServerHost + " on port " + pixiServerPort);

            // Connect to SICK-TIC server
            sickTicSocket = new Socket(sickTicServerHost, sickTicServerPort);
            System.out.println("Connected to SICK-TIC server: " + sickTicServerHost + " on port " + sickTicServerPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes the CSV file and converts rows into JSON objects.
     * @param filePath Path to the CSV file.
     * @return A list of JSON records.
     * @throws IOException if the file reading fails.
     */
    private static List<Record> processCSV(String filePath) throws IOException {
        List<Record> records = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String[] headers = reader.readLine().split(","); // Read the CSV headers
            long transactionDate=-1;
            // Read each row in the CSV file
            while ((line = reader.readLine()) != null) {
                System.out.println("Processing line : " +  line);
                String[] values = line.split(",");
                ObjectNode jsonObject = objectMapper.createObjectNode();

                // Map each CSV column to its corresponding value
                for (int i = 0; i < headers.length; i++) {
                    jsonObject.put(headers[i], values[i]);

                    // Check if the current header is "transaction_date" and store the value
                    if (headers[i].equalsIgnoreCase("TRANSACTION_DATE")) {
                        try {
                            transactionDate = Long.parseLong(values[i].trim());
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid TRANSACTION_DATE in line: " + line);
                            continue; // Skip this record
                        }
                    }
                }
            // Create a Record object containing transactionDate and jsonObject
            Record record = new Record(transactionDate, jsonObject);
            records.add(record);
            }
        }
        return records;
  }

    /**
     * Sends JSON data to the specified server (either PIXI or SICK-TIC).
     * @param serverSocket The socket connected to the server.
     * @param jsonRecord The JSON object representing the transaction.
     * @param serverType The type of the server (PIXI or SICK-TIC).
     */
    private static void sendDataToServer(Socket serverSocket, String jsonRecord, String serverType) {
        if (serverSocket == null) {
            System.out.println("Connection to " + serverType + " server is not available.");
            return;
        }

        try {
            OutputStream output = serverSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(jsonRecord);
            System.out.println("Sent data to " + serverType + " server: " + serverSocket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
