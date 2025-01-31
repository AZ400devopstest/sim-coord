package net.neology.tolling.tzc.simulator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.pojo.VehicleData;
import net.neology.tolling.tzc.simulator.service.CoordinatorService;
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
public class CoordinatorController {

    private final CoordinatorService coordinatorService;

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    Integer checkQueueSize() {
        log.info("Got request to check size of queue..");
        return coordinatorService.checkQueueSize();
    }

    @PostMapping(
            path = "/loadFromFile/{fileName}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void loadFromFile(@PathVariable String fileName) {
        log.info("Got request to load from file..");
        coordinatorService.queueMessagesFromFile(fileName);
    }

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void save(@RequestBody VehicleData vehicleData) {
        log.info("Got request to save..");
        coordinatorService.queueMessages(List.of(vehicleData));
    }

    @PostMapping(
            path = "/batch",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void saveAll(@RequestBody List<VehicleData> vehicleDataList) {
        log.info("Got request to saveAll..");
        coordinatorService.queueMessages(vehicleDataList);
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    void sendNow() {
        log.info("Got request to broadcast all queued messages now..");
        coordinatorService.sendMessagesNow();
    }

    @PutMapping(
            path = "/loadFromFile/{fileName}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void sendNowAndReload(@PathVariable String fileName) {
        log.info("Got request to broadcast and reload queued messages now..");
        coordinatorService.sendMessagesNow();
        coordinatorService.queueMessagesFromFile(fileName);
    }

    @PutMapping(
            path = "",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void updateFilePath(@RequestBody String newFilePath) {
        log.info("Got request to update file path..");
        coordinatorService.setFilePath(newFilePath);
    }
}
