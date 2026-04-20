package com.formforge.exception;

import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GraphQLExceptionHandlerTest {

    private final GraphQLExceptionHandler handler = new GraphQLExceptionHandler();
    private final DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

    @Test
    void resolvesApplicationNotFoundException() {
        var ex = new ApplicationNotFoundException(42L);
        var error = handler.resolveToSingleError(ex, env);

        assertThat(error).isNotNull();
        assertThat(error.getMessage()).isEqualTo("Application not found with id: 42");
    }

    @Test
    void resolvesIllegalArgumentException() {
        var ex = new IllegalArgumentException("bad input");
        var error = handler.resolveToSingleError(ex, env);

        assertThat(error).isNotNull();
        assertThat(error.getMessage()).isEqualTo("bad input");
    }

    @Test
    void returnsNullForOtherExceptions() {
        var ex = new RuntimeException("unexpected");
        var error = handler.resolveToSingleError(ex, env);

        assertThat(error).isNull();
    }

    @Test
    void applicationNotFoundException_hasCorrectMessage() {
        var ex = new ApplicationNotFoundException(42L);
        assertThat(ex.getMessage()).isEqualTo("Application not found with id: 42");
    }
}
