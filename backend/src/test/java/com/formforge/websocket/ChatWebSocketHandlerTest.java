package com.formforge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formforge.model.ChatMessage;
import com.formforge.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    @Mock
    private ChatMessageRepository chatRepo;

    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(chatRepo);
    }

    // ── afterConnectionEstablished ────────────────────────────────────────────

    @Test
    void afterConnectionEstablished_sendsHistory() throws Exception {
        List<ChatMessage> history = List.of(
                new ChatMessage(null, 1L, "alice", "hello", Instant.now()));
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(history);

        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);

        assertEquals(1, session.messages.size());
        String json = ((TextMessage) session.messages.get(0)).getPayload();
        assertTrue(json.contains("HISTORY"));
        assertTrue(json.contains("hello"));
    }

    @Test
    void afterConnectionEstablished_handlesIoException_gracefully() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        FakeSession failSession = new FakeSession("fail");
        failSession.failOnSend = true;

        assertDoesNotThrow(() -> handler.afterConnectionEstablished(failSession));
    }

    @Test
    void afterConnectionEstablished_serializeError_doesNotCrash() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        // Inject a broken ObjectMapper before connecting
        ObjectMapper broken = mock(ObjectMapper.class);
        when(broken.writeValueAsString(any())).thenThrow(
                new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        setField(handler, "objectMapper", broken);

        FakeSession session = new FakeSession("s1");
        assertDoesNotThrow(() -> handler.afterConnectionEstablished(session));
    }

    // ── afterConnectionClosed ─────────────────────────────────────────────────

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);

        // Close should not throw
        assertDoesNotThrow(() -> handler.afterConnectionClosed(session, CloseStatus.NORMAL));
    }

    // ── handleTextMessage ─────────────────────────────────────────────────────

    @Test
    void handleTextMessage_validChatMessage_savesAndBroadcasts() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        ChatMessage saved = new ChatMessage(null, 1L, "alice", "hello", Instant.now());
        when(chatRepo.save(any())).thenReturn(saved);

        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);
        session.messages.clear(); // clear history message

        handler.handleMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"userId\":1,\"username\":\"alice\",\"text\":\"hello\"}"));

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatRepo).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("alice", captor.getValue().getUsername());
        assertEquals("hello", captor.getValue().getText());

        assertEquals(1, session.messages.size());
        assertTrue(((TextMessage) session.messages.get(0)).getPayload().contains("CHAT_MESSAGE"));
    }

    @Test
    void handleTextMessage_wrongType_ignored() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);
        session.messages.clear();

        handler.handleMessage(session, new TextMessage(
                "{\"type\":\"PING\",\"userId\":1,\"username\":\"alice\",\"text\":\"hi\"}"));

        verify(chatRepo, never()).save(any());
        assertEquals(0, session.messages.size());
    }

    @Test
    void handleTextMessage_emptyText_ignored() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);
        session.messages.clear();

        handler.handleMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"userId\":1,\"username\":\"alice\",\"text\":\"  \"}"));

        verify(chatRepo, never()).save(any());
    }

    @Test
    void handleTextMessage_invalidJson_doesNotThrow() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        FakeSession session = new FakeSession("s1");
        handler.afterConnectionEstablished(session);

        assertDoesNotThrow(() ->
                handler.handleMessage(session, new TextMessage("not-json")));
    }

    // ── broadcast ─────────────────────────────────────────────────────────────

    @Test
    void broadcast_removesClosedSessions() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        ChatMessage saved = new ChatMessage(null, 1L, "a", "hi", Instant.now());
        when(chatRepo.save(any())).thenReturn(saved);

        FakeSession open = new FakeSession("open");
        FakeSession closed = new FakeSession("closed");
        closed.open = false;

        handler.afterConnectionEstablished(open);
        handler.afterConnectionEstablished(closed);
        open.messages.clear();
        closed.messages.clear();

        handler.handleMessage(open, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"userId\":1,\"username\":\"a\",\"text\":\"hi\"}"));

        assertEquals(1, open.messages.size());
        assertEquals(0, closed.messages.size());
    }

    @Test
    void broadcast_handlesIoException_gracefully() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        ChatMessage saved = new ChatMessage(null, 1L, "a", "hi", Instant.now());
        when(chatRepo.save(any())).thenReturn(saved);

        FakeSession failing = new FakeSession("fail");
        failing.failOnSend = true;
        FakeSession good = new FakeSession("good");

        handler.afterConnectionEstablished(failing);
        handler.afterConnectionEstablished(good);
        good.messages.clear();

        assertDoesNotThrow(() ->
                handler.handleMessage(good, new TextMessage(
                        "{\"type\":\"CHAT_MESSAGE\",\"userId\":1,\"username\":\"a\",\"text\":\"hi\"}")));
    }

    @Test
    void broadcast_serializeError_doesNotCrash() throws Exception {
        when(chatRepo.findTop50ByOrderByTimestampAsc()).thenReturn(List.of());
        ChatMessage saved = new ChatMessage(null, 1L, "alice", "hi", Instant.now());
        when(chatRepo.save(any())).thenReturn(saved);

        FakeSession session = new FakeSession("s1");
        // Use real mapper for establishing connection (sends history)
        handler.afterConnectionEstablished(session);
        session.messages.clear();

        // Now inject a broken mapper so broadcast serialize fails
        ObjectMapper broken = mock(ObjectMapper.class);
        when(broken.readTree(anyString())).thenReturn(
                new ObjectMapper().readTree(
                        "{\"type\":\"CHAT_MESSAGE\",\"userId\":1,\"username\":\"alice\",\"text\":\"hi\"}"));
        when(broken.writeValueAsString(any())).thenThrow(
                new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        setField(handler, "objectMapper", broken);

        assertDoesNotThrow(() ->
                handler.handleMessage(session, new TextMessage(
                        "{\"type\":\"CHAT_MESSAGE\",\"userId\":1,\"username\":\"alice\",\"text\":\"hi\"}")));
    }

    // ── Fake WebSocketSession ─────────────────────────────────────────────────

    static class FakeSession implements WebSocketSession {
        final String id;
        final List<WebSocketMessage<?>> messages = new ArrayList<>();
        boolean open = true;
        boolean failOnSend = false;

        FakeSession(String id) { this.id = id; }

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
        @Override public void setTextMessageSizeLimit(int l) {}
        @Override public int getTextMessageSizeLimit() { return 0; }
        @Override public void setBinaryMessageSizeLimit(int l) {}
        @Override public int getBinaryMessageSizeLimit() { return 0; }
        @Override public List<WebSocketExtension> getExtensions() { return List.of(); }
        @Override public void close() {}
        @Override public void close(CloseStatus status) {}
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
