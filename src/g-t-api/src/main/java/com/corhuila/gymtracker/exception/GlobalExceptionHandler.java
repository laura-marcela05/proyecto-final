package com.corhuila.gymtracker.exception;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return respond(ErrorResponse.of(ex.getCode()), ex.getCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.FieldDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        return respond(ErrorResponse.validation(details), ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody() {
        return respond(ErrorResponse.validation(List.of()), ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleWrongType(MethodArgumentTypeMismatchException ex) {
        ErrorResponse body = ErrorResponse.validation(
                List.of(new ErrorResponse.FieldDetail(ex.getName(), "Invalid value")));
        return respond(body, ErrorCode.VALIDATION_ERROR);
    }

    // A known path with the wrong verb is answered like an unknown route: the docs define no 405 code.
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleUnknownRoute() {
        return respond(ErrorResponse.of(ErrorCode.ROUTE_NOT_FOUND), ErrorCode.ROUTE_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        LOG.error("Unexpected error", ex);
        return respond(ErrorResponse.of(ErrorCode.INTERNAL_ERROR), ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorResponse body, ErrorCode code) {
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
