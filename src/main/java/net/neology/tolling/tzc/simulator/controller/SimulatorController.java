package net.neology.tolling.tzc.simulator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.pojo.VehicleData;
import net.neology.tolling.tzc.simulator.server.WebSocketServerHandler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coordinator")
public class SimulatorController {

    private final WebSocketServerHandler webSocketServerHandler;

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    Integer checkQueueSize() {
        log.info("Got request to check size of queue..");
        return webSocketServerHandler.checkQueueSize();
    }

    @PostMapping(
            path = "/loadFromFile/{fileName}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void loadFromFile(@PathVariable String fileName) {
        log.info("Got request to load from file..");
        webSocketServerHandler.queueMessagesFromFile(fileName);
    }

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void save(@RequestBody VehicleData vehicleData) {
        log.info("Got request to save..");
        webSocketServerHandler.queueMessages(List.of(vehicleData));
    }

    @PostMapping(
            path = "/batch",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void saveAll(@RequestBody List<VehicleData> vehicleDataList) {
        log.info("Got request to saveAll..");
        webSocketServerHandler.queueMessages(vehicleDataList);
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    void sendNow() {
        log.info("Got request to broadcast all queued messages now..");
        webSocketServerHandler.sendMessagesNow();
    }

    @PutMapping(
            path = "/loadFromFile/{fileName}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void sendNowAndReload(@PathVariable String fileName) {
        log.info("Got request to broadcast and reload queued messages now..");
        webSocketServerHandler.sendMessagesNow();
        webSocketServerHandler.queueMessagesFromFile(fileName);
    }
}
