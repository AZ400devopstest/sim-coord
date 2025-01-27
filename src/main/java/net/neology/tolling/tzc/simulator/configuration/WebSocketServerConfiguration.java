package net.neology.tolling.tzc.simulator.configuration;

import lombok.RequiredArgsConstructor;
import net.neology.tolling.tzc.simulator.server.WebSocketServerHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketServerConfiguration implements WebSocketConfigurer {

    @Bean
    public WebSocketServerHandler webSocketHandler() {
        return new WebSocketServerHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/websocket");
    }
}
