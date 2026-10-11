package com.byfood.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapNotFoundTo404() {
        ProblemDetail problem = handler.handleNotFound(new NotFoundException("Restaurante não configurado"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("Restaurante não configurado");
    }

    @Test
    void shouldMapValidationTo400() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "item");
        binding.addError(new FieldError("item", "name", "nome é obrigatório"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);
        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties().get("errors"))
                .isEqualTo(Map.of("name", "nome é obrigatório"));
    }

    @Test
    void shouldMapTypeMismatchTo400() {
        var ex = new MethodArgumentTypeMismatchException("abc", UUID.class, "trackingCode", null, null);

        ProblemDetail problem = handler.handleTypeMismatch(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).contains("trackingCode");
    }

    @Test
    void shouldKeepStatusOfSpringMvcErrors() {
        var ex = new HttpRequestMethodNotSupportedException(HttpMethod.PATCH.name());

        ProblemDetail problem = handler.handleGeneric(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @Test
    void shouldHideDetailsOfUnexpectedErrors() {
        ProblemDetail problem = handler.handleGeneric(new IllegalStateException("db password is 123"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("Erro inesperado");
    }
}
