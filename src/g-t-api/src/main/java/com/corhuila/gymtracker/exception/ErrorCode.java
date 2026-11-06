package com.corhuila.gymtracker.exception;

import org.springframework.http.HttpStatus;

/** Every error code of the API, with its HTTP status and default message (rest-conventions.md). */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Missing, invalid or expired token"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Your role may not perform this action"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Student not found"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Session not found"),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Route not found"),
    DOCUMENT_ALREADY_REGISTERED(HttpStatus.CONFLICT, "A student with this document already exists"),
    USERNAME_ALREADY_TAKEN(HttpStatus.CONFLICT, "A user with this username already exists"),
    SESSION_ALREADY_OPEN(HttpStatus.CONFLICT, "Student already has an open session"),
    SESSION_ALREADY_CLOSED(HttpStatus.CONFLICT, "Session already closed"),
    CANNOT_DELETE_OWN_USER(HttpStatus.CONFLICT, "An admin cannot delete their own user"),
    STUDENT_HAS_SESSIONS(HttpStatus.CONFLICT, "Delete the student's sessions first"),
    USER_HAS_STUDENTS(HttpStatus.CONFLICT, "The trainer still owns students"),
    MUSCLE_GROUP_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY,
            "muscleGroup must be one of: chest, back, legs, arms, shoulders, core"),
    MUSCLE_GROUPS_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Log at least one muscle group before checking out"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
