package com.formforge.config;

import com.formforge.websocket.ApplicationWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
class WebSocketConfigTest {

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Autowired
    private ApplicationWebSocketHandler handler;

    @Test
    void configurationLoads() {
        assertNotNull(webSocketConfig);
    }

    @Test
    void registersHandlerAtCorrectPath() {
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class, RETURNS_DEEP_STUBS);

        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/ws/applications");
    }
}
