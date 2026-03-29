package com.rs.payments.wallet.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should return 400 for validation errors")
    void shouldReturn400ForValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("user", "username", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().get("username"));
    }

    @Test
    @DisplayName("Should return 400 for duplicate resource")
    void shouldReturn400ForDuplicateResource() {
        DuplicateResourceException ex = new DuplicateResourceException("User already has a wallet");

        ResponseEntity<Map<String, String>> response = handler.handleDuplicate(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User already has a wallet", response.getBody().get("error"));
    }

    @Test
    @DisplayName("Should return 409 for user already exists")
    void shouldReturn409ForUserAlreadyExists() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Username already exists");

        ResponseEntity<Map<String, String>> response = handler.handleUserAlreadyExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username already exists", response.getBody().get("error"));
    }

    @Test
    @DisplayName("Should return 404 for resource not found")
    void shouldReturn404ForResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<Map<String, String>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().get("error"));
    }

    @Test
    @DisplayName("Should return 400 for illegal argument")
    void shouldReturn400ForIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Insufficient funds");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Insufficient funds", response.getBody().get("error"));
    }
}