package com.skillstorm.engagement.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatus_mapsStatusAndReasonFromException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Engagement 1 not found");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Engagement 1 not found");
    }

    @Test
    void handleBadRequest_returns400WithExceptionMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("startDate must not be after targetEndDate");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("startDate must not be after targetEndDate");
    }

    @Test
    void handleValidation_joinsAllFieldErrorsIntoMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "engagementName", "engagementName is required"),
                new FieldError("request", "clientId", "clientId is required")
        ));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .isEqualTo("engagementName: engagementName is required; clientId: clientId is required");
    }

    @Test
    void handleUnreadable_returns400WithCauseMessage() {
        Throwable cause = new RuntimeException("Cannot deserialize value");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("Malformed request body").contains("Cannot deserialize value");
    }
}
