package com.corhuila.gymtracker.exception;

/** A business rule or ownership failure, carrying the error code the client receives. */
public class ApiException extends RuntimeException {

    private final transient ErrorCode code;

    public ApiException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
