package com.formforge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formforge.model.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ApplicationWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket disconnected: {}", session.getId());
    }

    public void broadcastNewApplication(Application application) {
        Map<String, Object> message = Map.of(
                "type", "APPLICATION_CREATED",
                "payload", application
        );
        broadcast(message);
    }

    public void broadcastGeneratorStopped() {
        Map<String, Object> message = Map.of(
                "type", "GENERATOR_STOPPED"
        );
        broadcast(message);
    }

    private void broadcast(Object message) {
        sessions.removeIf(s -> !s.isOpen());
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            log.error("Failed to serialize WebSocket message", e);
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to {}", session.getId(), e);
            }
        }
    }
}
