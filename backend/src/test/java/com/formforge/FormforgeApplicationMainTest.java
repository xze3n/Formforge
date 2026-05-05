package com.formforge;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FormforgeApplicationMainTest {

    @Test
    void main_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);
            mocked.when(() -> SpringApplication.run(any(Class.class), any(String[].class)))
                  .thenReturn(ctx);

            FormforgeApplication.main(new String[]{});

            mocked.verify(() -> SpringApplication.run(FormforgeApplication.class, new String[]{}));
        }
    }
}
