package net.neology.tolling.tzc.simulator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;


@Configuration
@EnableIntegration
@IntegrationComponentScan
public class TcpClientConfiguration {

    public static final String DEFAULT_DATA_FILE_NAME = "VehicleSimulationData.csv";

    @MessagingGateway(defaultRequestChannel = "toTcp.input")
    public interface ToTcp {

        void send(String data, @Header("host") String host, @Header("port") int port) throws MessagingException;

    }

    @Bean
    public IntegrationFlow toTcp() {
        return flow -> flow.route(new TcpRouter());
    }

    @Bean
    public TcpNetServerConnectionFactory cfOne() {
        return new TcpNetServerConnectionFactory(4224);
    }

    @Bean
    public TcpReceivingChannelAdapter inOne(TcpNetServerConnectionFactory cfOne) {
        TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
        adapter.setConnectionFactory(cfOne);
        adapter.setOutputChannel(outputChannel());
        return adapter;
    }
    @Bean
    public QueueChannel outputChannel() {
        return new QueueChannel();
    }
}
