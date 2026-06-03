package com.formforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.formforge.websocket.ApplicationWebSocketHandler;
import com.formforge.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ApplicationWebSocketHandler handler;
    private final ChatWebSocketHandler chatHandler;

    @Value("${cors.allowed-origins:https://localhost:5173,https://localhost:5174}")
    private String allowedOriginsRaw;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOriginsRaw.split(",");
        registry.addHandler(handler, "/ws/applications")
                .setAllowedOrigins(origins);
        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins(origins);
    }
}
