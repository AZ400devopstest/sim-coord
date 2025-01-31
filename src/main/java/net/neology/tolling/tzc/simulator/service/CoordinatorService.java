package net.neology.tolling.tzc.simulator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.pojo.VehicleData;
import net.neology.tolling.tzc.simulator.repository.InMemoryVehicleDataRepository;
import net.neology.tolling.tzc.simulator.repository.VehicleDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class CoordinatorService {

    private static final String DEFAULT_DATA_FILE_NAME = "VehicleSimulationData.csv";
    private static final Integer MAX_RETRY_COUNT = 2;
    private static final Integer RETRY_SLEEP_MS = 500;

    // TODO: Revisit and load runtime parameters from config file
    @Value("${simulators.pixi.autostart:false}")
    private Boolean autoStart;
    @Value("${simulators.pixi.files.name:VehicleSimulationData.csv}")
    private String fileName;
    @Value("${simulators.pixi.files.path:~/tmp/}")
    private String filePath;
    @Value("${simulators.pixi.server.host:localhost}")
    private String pixiServerHost;
    @Value("${simulators.pixi.server.port:1234}")
    private String pixiServerPort;

    private final ObjectMapper jacksonObjectMapper;
    private final TaskScheduler taskScheduler;
    private final VehicleDataRepository vehicleDataRepository;

    public CoordinatorService(ObjectMapper mapper, TaskScheduler scheduler) {
        this.jacksonObjectMapper = mapper;
        this.taskScheduler = scheduler;
        this.vehicleDataRepository = new InMemoryVehicleDataRepository();
    }

    // TODO: Call from controller GET method
    public Integer checkQueueSize() {
        return vehicleDataRepository.currentSize();
    }

    /**
     *
     * @param vehicleDataList the vehicle transaction data to add to the queue
     */
    public void queueMessages(List<VehicleData> vehicleDataList) {
        log.info("Server queues [{}] messages..", vehicleDataList.size());
        vehicleDataRepository.saveAll(vehicleDataList);
        log.info("[{}] messages available in queue", vehicleDataRepository.currentSize());
    }

    public void sendMessagesNow() {
        log.info("sendMessagesNow..");
        sendMessages();
    }

    /**
     * Locate and load a file containing simulated vehicle transaction data
     * @param fileName the name of the file to load
     */
    public void queueMessagesFromFile(String fileName) {
        log.info("Server queue messages from file: [{}]", fileName);
        try {
            File sampleFile = locateFile(fileName);
            CsvMapper mapper = new CsvMapper();
            try (MappingIterator<VehicleData> iterator =
                         mapper.readerFor(VehicleData.class)
                                 .with(CSV_SCHEMA)
                                 .readValues(sampleFile)
            ) {
                queueMessages(iterator.readAll());
            }
        } catch (IOException ex) {
            log.error("Error occurred while reading from file: [{}]", fileName, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Update the filePath variable
     * @param filePath the new filesystem location where data files are located
     */
    public void setFilePath(String filePath) {
        Path path = Path.of(filePath);
        if (Files.exists(path)
                && Files.isDirectory(path)) {
            if (filePath.contains("\\") && !filePath.endsWith("\\")) {
                this.filePath = filePath + "\\";
            } else if (filePath.contains("/") && !filePath.endsWith("/")) {
                this.filePath = filePath + "/";
            } else {
                this.filePath = filePath;
            }
        } else {
            log.warn("setFilePath() called with invalid parameter: [{}]", filePath);
        }
    }

    /** Try to locate the file on the filesystem at the path configured in application.yml
            ...if unsuccessful, try to find the file on the classpath
            ...if unsuccessful, try to load the default file (which should be packaged with the jar)
        @param fileName the name of the file to load
    */
    private File locateFile(String fileName) {
        return loadFileFromFileSystem(filePath + fileName)
                .or(() -> loadFileFromClasspath(fileName))
                .or(this::loadDefaultFileFromClasspath)
                .orElseThrow(() -> new RuntimeException("message about failed to find file")); // TODO: More informative message, handle in the scheduled exception handler?
    }

    private Optional<File> loadFileFromFileSystem(String absoluteFilePath) {
        log.debug("Checking filesystem for file: [{}]", absoluteFilePath);
        Resource fileResource = new FileSystemResource(absoluteFilePath);
        try {
            return Optional.of(fileResource.getFile());
        } catch (IOException e) {
            log.warn("File was not found on filesystem: [{}]", absoluteFilePath);
            return Optional.empty();
        }
    }

    private Optional<File> loadFileFromClasspath(String fileName) {
        log.debug("Checking classpath for file: [{}]", fileName);
        ClassPathResource classPathResource = new ClassPathResource("classpath:" + fileName);
        try {
            return Optional.of(classPathResource.getFile());
        } catch (IOException e) {
            log.warn("File was not found on classpath: [{}]", fileName);
            return Optional.empty();
        }
    }

    private Optional<File> loadDefaultFileFromClasspath() {
        log.debug("Checking classpath for default data file [{}] as a last resort..", DEFAULT_DATA_FILE_NAME);
        ClassPathResource classPathResource = new ClassPathResource("classpath:" + DEFAULT_DATA_FILE_NAME);
        try {
            return Optional.of(classPathResource.getFile());
        } catch (IOException e) {
            log.error("Default data file was not found on classpath: [{}]", DEFAULT_DATA_FILE_NAME);
            return Optional.empty();
        }
    }

    private void queueMessage(VehicleData vehicleData) {
        log.info("Server queues message: [{}]", vehicleData);
        vehicleDataRepository.save(vehicleData);
    }

    // pull all messages from the "repository" (queue) and schedule them
    private void sendMessages() {
        vehicleDataRepository.findAll().forEach(this::scheduleMessage);
    }

    // send the message to all active websocket sessions
    protected void sendMessage(TextMessage message) {
        log.info(
                "Server sending message to destination [{}] with payload: \n[{}]",
                pixiServerHost + ":" + pixiServerPort,
                message.getPayload()
        );
        try (Socket socket = createClientSocket(pixiServerHost, Integer.parseInt(pixiServerPort))) {
            socket.getOutputStream().write(message.getPayload().getBytes());
        } catch (IOException ex) {
            // TODO: retry once?
            log.warn(
                    "Failed to send message to destination [{}]",
                    pixiServerHost + ":" + pixiServerPort,
                    ex
            );
        }
    }

    private Socket createClientSocket(String host, int port) throws IOException {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                return new Socket(pixiServerHost, Integer.parseInt(pixiServerPort));
            } catch (UnknownHostException ex) {
                throw new IOException("Unknown host: " + pixiServerHost, ex);
            } catch (SocketException ex) {
                log.debug("Connection failed.." + ex.getMessage());
                retryCount++;
                if (retryCount < MAX_RETRY_COUNT) {
                    log.debug("Will retry after {}ms..", RETRY_SLEEP_MS);
                    try {
                        Thread.sleep(RETRY_SLEEP_MS);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Socket connection thread interrupted while sleeping/retrying", interruptedException);
                    }
                }
            }

        }
        throw new IOException("Socket connection refused after exhausting configured retry attempts..");
    }

//    private void scheduleMessages(List<VehicleData> vehicleData) {
//        // TODO: parse and remove the first token, delayTime
//        int delay = Integer.parseInt(vehicleData.timeDelay());
//        try {
//            String jsonPayload = jacksonObjectMapper.writeValueAsString(vehicleData);
//            TextMessage textMessage = new TextMessage(jsonPayload);
//            taskScheduler.schedule(() -> sendMessage(textMessage), Instant.now().plusMillis(delay));
//        } catch (JsonProcessingException ex) {
//            log.error("Unexpected exception while serializing to JSON, object: [{}]", vehicleData, ex);
//            // TODO: do anything?
//        }
//    }

    private void scheduleMessage(VehicleData vehicleData) {
        int delay = Integer.parseInt(vehicleData.timeDelay());
        try {
            String jsonPayload = jacksonObjectMapper.writeValueAsString(vehicleData);
            TextMessage textMessage = new TextMessage(jsonPayload);
            taskScheduler.schedule(() -> sendMessage(textMessage), Instant.now().plusMillis(delay));
        } catch (JsonProcessingException ex) {
            log.error("Unexpected exception while serializing to JSON, object: [{}]", vehicleData, ex);
            // TODO: do anything?
        }
    }

    @PostConstruct
    private void init() {
        if (autoStart) {
            queueMessagesFromFile(DEFAULT_DATA_FILE_NAME);
            // TODO: Load the data file as defined in config, defaulting to VehicleSimulationData.csv on the classpath
            // TODO: i.e. first try to find FileResource, then ClasspathResource if no joy
            sendMessages();
        }
    }

    // TODO: Refactor once the schema has been established/agreed upon
    private static final CsvSchema CSV_SCHEMA =
            CsvSchema.builder()
                    .addColumn("timeDelay", CsvSchema.ColumnType.STRING)
                    .addColumn("facilityId", CsvSchema.ColumnType.STRING)
                    .addColumn("tollPoint", CsvSchema.ColumnType.STRING)
                    .addColumn("lane", CsvSchema.ColumnType.STRING)
                    .addColumn("vehicleId", CsvSchema.ColumnType.STRING)
                    .addColumn("vehicleClass", CsvSchema.ColumnType.STRING)
                    .addColumn("confidence", CsvSchema.ColumnType.STRING)
                    .addColumn("length", CsvSchema.ColumnType.STRING)
                    .addColumn("width", CsvSchema.ColumnType.STRING)
                    .addColumn("height", CsvSchema.ColumnType.STRING)
                    .addColumn("axleCount", CsvSchema.ColumnType.STRING)
                    .addColumn("speed", CsvSchema.ColumnType.STRING)
                    .addColumn("direction", CsvSchema.ColumnType.STRING)
                    .addColumn("plateText", CsvSchema.ColumnType.STRING)
                    .addColumn("plateReadConfidence", CsvSchema.ColumnType.STRING)
                    .addColumn("plateNationality", CsvSchema.ColumnType.STRING)
                    .addColumn("platePlateFinderConfidence", CsvSchema.ColumnType.STRING)
                    .addColumn("plateXpos", CsvSchema.ColumnType.STRING)
                    .addColumn("plateWidth", CsvSchema.ColumnType.STRING)
                    .addColumn("plateYpos", CsvSchema.ColumnType.STRING)
                    .addColumn("plateHeight", CsvSchema.ColumnType.STRING)
                    .addColumn("tagId", CsvSchema.ColumnType.STRING)
                    .addColumn("tid", CsvSchema.ColumnType.STRING)
                    .addColumn("tagType", CsvSchema.ColumnType.STRING)
                    .addColumn("antenna", CsvSchema.ColumnType.STRING)
                    .addColumn("rssi", CsvSchema.ColumnType.STRING)
                    .addColumn("txPower", CsvSchema.ColumnType.STRING)
                    .addColumn("userData", CsvSchema.ColumnType.STRING)
                    .build().withHeader();
}
