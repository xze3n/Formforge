package com.formforge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationWebSocketHandlerTest {

    private ApplicationWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new ApplicationWebSocketHandler();
        objectMapper = new ObjectMapper();
    }

    @Test
    void afterConnectionEstablished_addsSession() throws Exception {
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);

        handler.broadcastGeneratorStopped();
        assertEquals(1, session.messages.size());
    }

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        handler.broadcastGeneratorStopped();
        assertEquals(0, session.messages.size());
    }

    @Test
    void broadcastNewApplication_sendsCorrectMessage() throws Exception {
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);

        Application app = new Application(1L, ApplicationType.MERIT, "2025/2026",
                Semester.I, "4/20/2026", ApplicationStatus.DRAFT);

        handler.broadcastNewApplication(app);

        assertEquals(1, session.messages.size());
        String json = ((TextMessage) session.messages.get(0)).getPayload();

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertEquals("APPLICATION_CREATED", parsed.get("type"));
        assertNotNull(parsed.get("payload"));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) parsed.get("payload");
        assertEquals(1, ((Number) payload.get("id")).intValue());
        assertEquals("Merit", payload.get("type"));
        assertEquals("2025/2026", payload.get("academicYear"));
        assertEquals("I", payload.get("semester"));
        assertEquals("Draft", payload.get("status"));
        assertEquals("4/20/2026", payload.get("createdAt"));
    }

    @Test
    void broadcastGeneratorStopped_sendsCorrectMessage() throws Exception {
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);

        handler.broadcastGeneratorStopped();

        assertEquals(1, session.messages.size());
        String json = ((TextMessage) session.messages.get(0)).getPayload();

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertEquals("GENERATOR_STOPPED", parsed.get("type"));
        assertFalse(parsed.containsKey("payload"));
    }

    @Test
    void broadcast_sendsToMultipleSessions() throws Exception {
        FakeSession session1 = new FakeSession("s1");
        FakeSession session2 = new FakeSession("s2");

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        handler.broadcastGeneratorStopped();

        assertEquals(1, session1.messages.size());
        assertEquals(1, session2.messages.size());
    }

    @Test
    void broadcast_removesClosedSessions() throws Exception {
        FakeSession openSession = new FakeSession("open");
        FakeSession closedSession = new FakeSession("closed");

        handler.afterConnectionEstablished(openSession);
        handler.afterConnectionEstablished(closedSession);

        closedSession.open = false;

        handler.broadcastGeneratorStopped();

        assertEquals(1, openSession.messages.size());
        assertEquals(0, closedSession.messages.size());
    }

    @Test
    void broadcast_handlesIOException_gracefully() throws Exception {
        FakeSession failingSession = new FakeSession("failing");
        failingSession.failOnSend = true;
        FakeSession goodSession = new FakeSession("good");

        handler.afterConnectionEstablished(failingSession);
        handler.afterConnectionEstablished(goodSession);

        assertDoesNotThrow(() -> handler.broadcastGeneratorStopped());
        assertEquals(1, goodSession.messages.size());
    }

    // ── Simple fake WebSocketSession for testing ──────────────────────

    static class FakeSession implements WebSocketSession {
        final String id;
        final List<WebSocketMessage<?>> messages = new ArrayList<>();
        boolean open = true;
        boolean failOnSend = false;

        FakeSession(String id) {
            this.id = id;
        }

        @Override public String getId() { return id; }
        @Override public boolean isOpen() { return open; }

        @Override
        public void sendMessage(WebSocketMessage<?> message) throws IOException {
            if (failOnSend) throw new IOException("simulated failure");
            messages.add(message);
        }

        @Override public URI getUri() { return null; }
        @Override public HttpHeaders getHandshakeHeaders() { return new HttpHeaders(); }
        @Override public Map<String, Object> getAttributes() { return Map.of(); }
        @Override public Principal getPrincipal() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return null; }
        @Override public InetSocketAddress getRemoteAddress() { return null; }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int messageSizeLimit) {}
        @Override public int getTextMessageSizeLimit() { return 0; }
        @Override public void setBinaryMessageSizeLimit(int messageSizeLimit) {}
        @Override public int getBinaryMessageSizeLimit() { return 0; }
        @Override public List<WebSocketExtension> getExtensions() { return List.of(); }
        @Override public void close() {}
        @Override public void close(CloseStatus status) {}
    }
}
