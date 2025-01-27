package net.neology.tolling.tzc.simulator.server;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.pojo.VehicleData;
import net.neology.tolling.tzc.simulator.repository.InMemoryVehicleDataRepository;
import net.neology.tolling.tzc.simulator.repository.VehicleDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class WebSocketServerHandler extends TextWebSocketHandler {

    @Value("${simulators.pixi.files.path:~/tmp/}")
    private String filePath;

    private final Set<WebSocketSession> sessions;
    private final VehicleDataRepository vehicleDataRepository;

    public WebSocketServerHandler() {
        this(new InMemoryVehicleDataRepository()); // TODO: revisit and refactor after we stop using jank in-memory DB
    }
    private WebSocketServerHandler(final VehicleDataRepository vehicleDataRepository) {
        this.sessions = ConcurrentHashMap.newKeySet();
        this.vehicleDataRepository = vehicleDataRepository;
    }

    @Scheduled(cron = "${scheduling.default.cron}")
    void sendScheduledMessages() {
        log.info("Scheduled sendMessages...");
        sendMessages();
    }

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
        sessions.add(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Server received message: \n[{}]", message.getPayload());
        queueMessagesFromCsvString(message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn(
                "Server transport error for session: [{}], message: {}",
                session.getRemoteAddress(),
                exception.getMessage(),
                exception
        );
        // TODO: close session?
    }

    public Integer checkQueueSize() {
        return vehicleDataRepository.currentSize();
    }

    @Async
    public void queueMessagesFromFile(String fileName) {
        log.info("Server queue messages from file: [{}]", fileName);
        List<VehicleData> vehicles;
        Resource fileResource = new FileSystemResource(filePath + fileName);
        try {
            File sampleFile = fileResource.getFile();
            CsvMapper mapper = new CsvMapper();
            try (MappingIterator<VehicleData> iterator =
                         mapper.readerFor(VehicleData.class)
                                 .with(CSV_SCHEMA)
                                 .readValues(sampleFile)
            ) {
                vehicles = new ArrayList<>(vehicleDataRepository.saveAll(iterator.readAll()));
            }
        } catch (IOException ex) {
            log.error("Error occurred while reading from file: [{}]", fileName, ex);
            throw new RuntimeException(ex);
        }

        log.info("{} vehicles saved", vehicles.size());
        log.info("{} vehicles stored in message queue", vehicleDataRepository.currentSize());
    }

    public void queueMessages(List<VehicleData> vehicleDataList) {
        vehicleDataList.forEach(this::queueMessage);
        log.info("Server finished saving {} message(s) to the queue", vehicleDataList.size());
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
            log.error("Exception while mapping CSV String to VehicleData object: {}", vehicleDataCsv, ex);
            throw new RuntimeException(ex);
        }
    }

    private VehicleData queueMessage(VehicleData vehicleData) {
        log.info("Server queue message: [{}]", vehicleData);
        return vehicleDataRepository.save(vehicleData);
    }

    // TODO: Refactor so that errors while transmitting don't force us throw the whole 'repository' of data away
    @Async
    protected void sendMessages() {
        List<TextMessage> messages =
                vehicleDataRepository.findAll()
                        .stream()
                        .map(vehicle -> new TextMessage(vehicle.toString()))
                        .toList();

        for (TextMessage message : messages) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    log.info("Server sending message to session [{}] with payload: \n[{}]", session.getRemoteAddress(), message.getPayload());
                    try {
                        session.sendMessage(message);
                    } catch (IOException ex) {
                        log.warn("Failed to send message to session [{}]", session.getRemoteAddress(), ex);
                    }
                }
            }
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
