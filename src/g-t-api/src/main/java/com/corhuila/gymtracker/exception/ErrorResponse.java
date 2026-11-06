package com.corhuila.gymtracker.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Body of every error: {error, message} plus details, which only VALIDATION_ERROR carries. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String message, List<FieldDetail> details) {

    public record FieldDetail(String field, String message) {
    }

    public static ErrorResponse of(ErrorCode code) {
        // VALIDATION_ERROR always carries "details", empty when no single field is to blame.
        List<FieldDetail> details = code == ErrorCode.VALIDATION_ERROR ? List.of() : null;
        return new ErrorResponse(code.name(), code.getMessage(), details);
    }

    public static ErrorResponse validation(List<FieldDetail> details) {
        return new ErrorResponse(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getMessage(), details);
    }
}
