package com.formforge.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.formforge.model.ChatMessage;
import com.formforge.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageRepository chatRepo;

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Chat WS connected: {}", session.getId());

        // Send message history to the newly connected client
        try {
            List<ChatMessage> history = chatRepo.findTop50ByOrderByTimestampAsc();
            String json = objectMapper.writeValueAsString(
                    Map.of("type", "HISTORY", "messages", history));
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("Failed to send chat history", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Chat WS disconnected: {}", session.getId());
    }

    // ── Incoming messages ────────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());

            if (!"CHAT_MESSAGE".equals(node.path("type").asText())) return;

            Long userId   = node.path("userId").asLong();
            String username = node.path("username").asText("unknown");
            String text   = node.path("text").asText("").trim();

            if (text.isEmpty()) return;

            ChatMessage saved = chatRepo.save(new ChatMessage(null, userId, username, text, Instant.now()));

            broadcast(Map.of("type", "CHAT_MESSAGE", "message", saved));

        } catch (IOException e) {
            log.error("Failed to process chat message from {}", session.getId(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void broadcast(Object payload) {
        sessions.removeIf(s -> !s.isOpen());
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            log.error("Failed to serialize chat broadcast", e);
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession s : sessions) {
            try {
                synchronized (s) {
                    s.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.error("Failed to send chat message to {}", s.getId(), e);
            }
        }
    }
}
