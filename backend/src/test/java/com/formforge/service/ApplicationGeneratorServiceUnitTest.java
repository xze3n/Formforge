package com.formforge.service;

import com.formforge.repository.ApplicationRepository;
import com.formforge.websocket.ApplicationWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationGeneratorServiceUnitTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private ApplicationWebSocketHandler webSocketHandler;

    @InjectMocks
    private ApplicationGeneratorService service;

    @Test
    void generateOne_exceptionFromSave_isCaughtGracefully() throws Exception {
        when(repository.save(any())).thenThrow(new RuntimeException("DB error"));

        Method m = ApplicationGeneratorService.class.getDeclaredMethod("generateOne");
        m.setAccessible(true);

        assertDoesNotThrow(() -> m.invoke(service));
    }
}
