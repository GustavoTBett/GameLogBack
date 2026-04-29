package com.gamelog.gamelog.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleEntityCannotBeNullShouldReturn404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleEntityCannotBeNull(new EntityCannotBeNull("Not found"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Not found", response.getBody().get("message"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleBadRequestShouldReturn400ForIllegalArgument() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBadRequest(new IllegalArgumentException("Invalid data"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid data", response.getBody().get("message"));
    }

    @Test
    void handleBadRequestShouldReturn400ForConstraintViolation() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBadRequest(new ConstraintViolationException("Bad constraint", null));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad constraint", response.getBody().get("message"));
    }

    @Test
    void handleValidationShouldUseFirstFieldErrorMessage() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "email", "must be valid")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("email: must be valid", response.getBody().get("message"));
    }

    @Test
    void handleRuntimeShouldReturn400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntime(new RuntimeException("Generic runtime error"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Generic runtime error", response.getBody().get("message"));
    }
}
