package com.formforge.config;

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

    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:5173",
        "http://localhost:5174"
    };

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/applications")
                .setAllowedOrigins(ALLOWED_ORIGINS);
        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins(ALLOWED_ORIGINS);
    }
}
