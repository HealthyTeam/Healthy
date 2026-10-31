package healthmonitor.notifications;

import healthmonitor.notifications.exception.ErrorResponse;
import healthmonitor.notifications.exception.GlobalExceptionHandler;
import healthmonitor.notifications.exception.MarkingAsReadException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleMarkingAsReadException() {
        // given
        MarkingAsReadException exception =
                new MarkingAsReadException("Access denied");

        when(request.getRequestURI()).thenReturn("/api/notifications/1");

        // when
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleMarkingAsReadException(exception, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals(
                "Error while processing this action",
                body.message()
        );
        assertEquals("/api/notifications/1", body.path());
        assertNotNull(body.timestamp());
    }

    @Test
    void shouldHandleValidationExceptions() {
        // given
        BindingResult bindingResult = new BeanPropertyBindingResult(
                new Object(),
                "object"
        );

        bindingResult.addError(
                new FieldError(
                        "object",
                        "email",
                        "Invalid email address"
                )
        );

        bindingResult.addError(
                new FieldError(
                        "object",
                        "username",
                        "Username is required"
                )
        );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        null,
                        bindingResult
                );

        // when
        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleValidationExceptions(exception);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, String> body = response.getBody();

        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals("Invalid email address", body.get("email"));
        assertEquals("Username is required", body.get("username"));
    }

    @Test
    void shouldHandleAccessDeniedException() {
        // given
        AuthorizationDeniedException exception =
                new AuthorizationDeniedException("Access denied");

        when(request.getRequestURI()).thenReturn("/api/users");

        // when
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleAccessDenied(exception, request);

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(403, body.status());
        assertEquals("Forbidden", body.error());
        assertEquals(
                "You do not have permission to access this resource",
                body.message()
        );
        assertEquals("/api/users", body.path());
        assertNotNull(body.timestamp());
    }

    @Test
    void shouldHandleGlobalException() {
        // given
        Exception exception = new RuntimeException("Database connection failed");

        when(request.getRequestURI()).thenReturn("/api/data");

        // when
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleGlobalException(exception, request);

        // then
        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
        assertEquals(
                "Unexpected server error Database connection failed",
                body.message()
        );
        assertEquals("/api/data", body.path());
        assertNotNull(body.timestamp());
    }
}