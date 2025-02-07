package net.neology.tolling.tzc.simulator.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


@Configuration
@EnableIntegration
@IntegrationComponentScan
public class TcpClientConfiguration {

    @MessagingGateway(defaultRequestChannel = "toTcp.input")
    public interface ToTcp {

        void send(String data, @Header("host") String host, @Header("port") int port);

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
