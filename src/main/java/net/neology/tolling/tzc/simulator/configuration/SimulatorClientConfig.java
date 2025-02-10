package net.neology.tolling.tzc.simulator.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Component
@ConfigurationProperties(prefix="simulators")
public class SimulatorClientConfig {
    private List<String> clients;
}
