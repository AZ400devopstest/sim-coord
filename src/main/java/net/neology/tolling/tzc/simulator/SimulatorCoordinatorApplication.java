package net.neology.tolling.tzc.simulator;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.service.CoordinatorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.integration.config.EnableMessageHistory;

@Log4j2
@RequiredArgsConstructor
@SpringBootApplication
@EnableMessageHistory
public class SimulatorCoordinatorApplication implements CommandLineRunner {

    private final CoordinatorService coordinatorService;

    public static void main(String[] args) {
        log.info("TZC Simulator Coordinator start..");
        new SpringApplicationBuilder(SimulatorCoordinatorApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        log.info("TZC Simulator Coordinator finish..");
        System.exit(0);
    }

    @Override
    public void run(String... args) {
        coordinatorService.sendMessagesNow();
    }
}
