package net.neology.tolling.tzc.simulator.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TcpRouter extends AbstractMessageRouter {

    @Autowired
    private IntegrationFlowContext flowContext;

    private final static int MAX_CACHED = 10;

    @SuppressWarnings("serial")
    private final LinkedHashMap<String, MessageChannel> subFlows =
            new LinkedHashMap<>(MAX_CACHED, .75f, true) {

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, MessageChannel> eldest) {
                    if (size() > MAX_CACHED) {
                        removeSubFlow(eldest);
                        return true;
                    }
                    else {
                        return false;
                    }
                }

            };

    @Override
    protected synchronized Collection<MessageChannel> determineTargetChannels(Message<?> message) {
        MessageChannel channel =
                this.subFlows.get(
                        message.getHeaders().get("host", String.class) +
                        message.getHeaders().get("port")
                );
        if (channel == null) {
            channel = createNewSubflow(message);
        }
        return Collections.singletonList(channel);
    }

    private MessageChannel createNewSubflow(Message<?> message) {
        String host = (String) message.getHeaders().get("host");
        Integer port = (Integer) message.getHeaders().get("port");
        Assert.state(host != null && port != null, "host and/or port header missing");
        String hostPort = host + port;

        TcpNetClientConnectionFactory cf = new TcpNetClientConnectionFactory(host, port);
        TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
        handler.setConnectionFactory(cf);
        IntegrationFlow flow = f -> f.handle(handler);
        IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
                this.flowContext.registration(flow)
                        .addBean(cf)
                        .id(hostPort + ".flow")
                        .register();
        MessageChannel inputChannel = flowRegistration.getInputChannel();
        this.subFlows.put(hostPort, inputChannel);
        return inputChannel;
    }

    private void removeSubFlow(Map.Entry<String, MessageChannel> eldest) {
        String hostPort = eldest.getKey();
        this.flowContext.remove(hostPort + ".flow");
    }

}