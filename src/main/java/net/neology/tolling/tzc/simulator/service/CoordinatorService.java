package net.neology.tolling.tzc.simulator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.configuration.TcpClientConfiguration;
import net.neology.tolling.tzc.simulator.pojo.VehicleData;
import net.neology.tolling.tzc.simulator.repository.VehicleDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class CoordinatorService {

    private static final String DEFAULT_DATA_FILE_NAME = "VehicleSimulationData.csv";

    // TODO: Revisit and load runtime parameters from config file
    @Value("${simulators.pixi.files.name:VehicleSimulationData.csv}")
    private String fileName;
    @Value("${simulators.pixi.files.path:~/tmp/}")
    private String filePath;

    @Value("${simulators.pixi.server.host:localhost}")
    private String serverHost;
    @Value("${simulators.pixi.server.port:1234}")
    private int serverPort;

    private final TcpClientConfiguration.ToTcp toTcp;
    private final ObjectMapper jacksonObjectMapper;
    private final VehicleDataRepository vehicleDataRepository;

    public CoordinatorService(
            TcpClientConfiguration.ToTcp toTcp,
            ObjectMapper jacksonObjectMapper,
            VehicleDataRepository vehicleDataRepository) {

        this.toTcp = toTcp;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.vehicleDataRepository = vehicleDataRepository;
    }

    // TODO:
    public void broadcastToClients(String input) {
        log.info("Broadcasting message: {}", input);
        try {
            toTcp.send(input, serverHost, serverPort);
        } catch (MessagingException ex) {
            log.warn("Unexpected exception caught: {}", ex.getMessage(), ex);
        }
    }

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
        queueMessagesFromFile(fileName);
        sendMessages();
    }

    @Scheduled(cron = "${scheduling.default.cron:0 */1 * * * ?}")
    public void scheduledSendMessages() {
        log.info("scheduledSendMessages...");
        queueMessagesFromFile(fileName);
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

    // pull all messages from the "repository" (queue) and schedule them
    private void sendMessages() {
        sendMessages(vehicleDataRepository.findAll());
    }

    private void sendMessages(List<VehicleData> vehicleDataList) {
        vehicleDataList.forEach(vehicleData -> {
            try {
                broadcastToClients(jacksonObjectMapper.writeValueAsString(vehicleData));
            } catch (JsonProcessingException ex) {
                log.warn("Unexpected error while converting object to JSON string: {}", vehicleData, ex);
            }
        });
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
