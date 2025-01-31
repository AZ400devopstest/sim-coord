package net.neology.tolling.tzc.simulator.configuration;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

@Log4j2
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class TcpServerSocketConfiguration {
}
