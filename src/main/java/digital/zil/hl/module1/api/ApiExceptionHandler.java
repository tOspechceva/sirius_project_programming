package digital.zil.hl.module1.api;

import digital.zil.hl.module1.api.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Преобразует исключения бизнес-логики в удобный HTTP-ответ.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(final DataIntegrityViolationException ex) {
        final Throwable cause = ex.getMostSpecificCause();
        final String details = cause != null ? cause.getMessage() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Конфликт данных (дубликат или ограничение БД): " + details));
    }
}
