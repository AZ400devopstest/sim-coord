package net.neology.tolling.tzc.simulator.server;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.pojo.VehicleData;
import net.neology.tolling.tzc.simulator.repository.InMemoryVehicleDataRepository;
import net.neology.tolling.tzc.simulator.repository.VehicleDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketServerHandler extends TextWebSocketHandler {

    private static final String DEFAULT_DATA_FILE_PATH = "classpath:VehicleSimulationData.csv";

    @Value("${simulators.pixi.files.path:~/tmp/}")
    private String filePath;

    @Value("${simulators.pixi.autostart:false}")
    private Boolean autoStart;

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

    private final Set<WebSocketSession> sessions;
    private final VehicleDataRepository vehicleDataRepository;

    @Autowired
    private TaskScheduler taskScheduler;

    public WebSocketServerHandler() {
        this(new InMemoryVehicleDataRepository()); // TODO: revisit and refactor after we stop using jank in-memory DB
    }
    private WebSocketServerHandler(final VehicleDataRepository vehicleDataRepository) {
        this.sessions = ConcurrentHashMap.newKeySet();
        this.vehicleDataRepository = vehicleDataRepository;
    }

//    @Scheduled(cron = "${scheduling.default.cron}")
//    void sendScheduledMessages() {
//        log.info("Scheduled sendMessages...");
//        sendMessages();
//    }

    public void sendMessagesNow() {
        log.info("Unscheduled sendMessages...");
        sendMessages();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Connection [{}] to server closed, status: [{}]", session.getRemoteAddress(), status);
        sessions.remove(session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("New server connection...");
        WebSocketSession newSession = new ConcurrentWebSocketSessionDecorator(session, 5000, 64 * 1024);
        sessions.add(newSession);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Server received message: \n[{}]", message.getPayload());
        queueMessagesFromCsvString(message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn(
                "Server transport error for session: [{}], message: [{}]",
                session.getRemoteAddress(),
                exception.getMessage(),
                exception
        );
        // TODO: close session?
    }

    public Integer checkQueueSize() {
        return vehicleDataRepository.currentSize();
    }

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
                vehicleDataRepository.saveAll(iterator.readAll());
                log.info("[{}] vehicles stored in message queue", vehicleDataRepository.currentSize());
            }
        } catch (IOException ex) {
            log.error("Error occurred while reading from file: [{}]", fileName, ex);
            throw new RuntimeException(ex);
        }
    }

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
        log.debug("Checking classpath for default data file [{}] as a last resort..", DEFAULT_DATA_FILE_PATH);
        ClassPathResource classPathResource = new ClassPathResource(DEFAULT_DATA_FILE_PATH);
        try {
            return Optional.of(classPathResource.getFile());
        } catch (IOException e) {
            log.error("Default data file was not found on classpath: [{}]", DEFAULT_DATA_FILE_PATH);
            return Optional.empty();
        }
    }


    public void queueMessages(List<VehicleData> vehicleDataList) {
        vehicleDataList.forEach(this::queueMessage);
        log.info("Server finished saving [{}] message(s) to the queue", vehicleDataList.size());
    }

    private void queueMessagesFromCsvString(String vehicleDataCsv) {
        CsvMapper mapper = new CsvMapper();
        try (MappingIterator<VehicleData> iterator =
                     mapper.readerFor(VehicleData.class)
                             .with(CSV_SCHEMA)
                             .readValues(vehicleDataCsv)
        ) {
            iterator.readAll().forEach(this::queueMessage); // TODO: Revisit and rewrite so that the whole method doesn't fail on a single bad record
        } catch (IOException ex) {
            log.error("Exception while mapping CSV String to VehicleData object: [{}]", vehicleDataCsv, ex);
            throw new RuntimeException(ex);
        }
    }

    private void queueMessage(VehicleData vehicleData) {
        log.info("Server queue message: [{}]", vehicleData);
        vehicleDataRepository.save(vehicleData);
    }

    // pull all messages from the "repository" (queue) and schedule them
    private void sendMessages() {
        vehicleDataRepository.findAll().forEach(this::scheduleMessage);
    }

    // send the message to all active websocket sessions
    @Async
    protected void sendMessage(WebSocketSession session, TextMessage message) {
        log.info("Server sending message to session [{}] with payload: \n[{}]", session.getRemoteAddress(), message.getPayload());
        try {
            session.sendMessage(message);
        } catch (IOException ex) {
            log.warn("Failed to send message to session [{}]", session.getRemoteAddress(), ex);
        }
    }

    private void scheduleMessage(VehicleData vehicleData) {
        // TODO: parse and remove the first token, delayTime
        int delay = Integer.parseInt(vehicleData.timeDelay());
        TextMessage textMessage = new TextMessage(vehicleData.toString());
        sessions.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session ->
                        taskScheduler.schedule(() ->
                                sendMessage(
                                        session,
                                        textMessage),
                                Instant.now().plusMillis(delay)));
    }

    @PostConstruct
    private void init() {
        if (autoStart) {
            // TODO: Load the data file as defined in config, defaulting to VehicleSimulationData.csv on the classpath
            // TODO: i.e. first try to find FileResource, then ClasspathResource if no joy
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
