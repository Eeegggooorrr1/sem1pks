package dasein.sem1pks.handler;

import dasein.sem1pks.dto.response.ErrorResponse;
import dasein.sem1pks.exception.DomainException;
import dasein.sem1pks.exception.notfound.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        if (ex instanceof NotFoundException) {
            log.info("Ресурс не найден: {}", ex.getMessage());
        } else {
            log.warn("Доменная ошибка [{}]: {}", ex.getErrorCode(), ex.getMessage());
        }
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : "_global";
            errors.computeIfAbsent(fieldName, ignored -> new ArrayList<>()).add(error.getDefaultMessage());
        });
        return buildErrorResponse("VALIDATION_ERROR", "Ошибка валидации данных", HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            errors.computeIfAbsent(field, ignored -> new ArrayList<>()).add(violation.getMessage());
        });
        return buildErrorResponse("VALIDATION_ERROR", "Ошибка валидации параметров", HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.warn("Некорректный JSON: {}", ex.getMessage());
        return buildErrorResponse("MALFORMED_JSON", "Некорректный формат JSON", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Параметр '%s' имеет неверный тип", ex.getName());
        return buildErrorResponse("TYPE_MISMATCH", message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = String.format("Отсутствует обязательный параметр '%s'", ex.getParameterName());
        return buildErrorResponse("MISSING_PARAMETER", message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        log.error("Ошибка доступа к данным", ex);
        return buildErrorResponse("DATABASE_ERROR", "Внутренняя ошибка при работе с базой данных", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Непредвиденная ошибка", ex);
        return buildErrorResponse("INTERNAL_ERROR", "Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String code, String message, HttpStatus status) {
        return buildErrorResponse(code, message, status, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String code, String message, HttpStatus status,
                                                              Map<String, List<String>> details) {
        ErrorResponse error = new ErrorResponse(code, message, status.value(), details);
        return ResponseEntity.status(status).body(error);
    }
}
