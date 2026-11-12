package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.response.ErrorResponse;
import com.fajekrina.universal.model.response.ErrorResponseBuilder;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.ZonedDateTime;

/**
 * Single error contract: {errorDesc, errorCode "SDUI-xxx", errorMessage, timestamp}
 * with the HTTP status carried by the fault.
 */
@Log4j2
@RestControllerAdvice
public class GlobalErrorHandler {

    static ErrorResponse body(UniversalFault fault, String message) {
        return ErrorResponseBuilder.builder()
                .errorDesc(fault.name())
                .errorCode(String.format("%s-%03d", Constants.SERVICE_PREFIX, fault.getErrorCode()))
                .errorMessage(message == null ? fault.getMessage() : message)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    private static ResponseEntity<ErrorResponse> respond(UniversalFault fault, String message, HttpStatusCode status) {
        return ResponseEntity.status(status).body(body(fault, message));
    }

    @ExceptionHandler(UniversalException.class)
    public ResponseEntity<ErrorResponse> universalError(UniversalException ex) {
        log.error("{}: Error Code {}: {}", ex.getClass().getSimpleName(), ex.getFault().getErrorCode(), ex.getMessage());
        return respond(ex.getFault(), ex.getMessage(), ex.getFault().getStatus());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> lockError(ObjectOptimisticLockingFailureException ex) {
        log.error("Optimistic lock failure", ex);
        return respond(UniversalFault.LOCK_CONFLICT, null, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrityError(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        return respond(UniversalFault.INTEGRITY_VIOLATION, ex.getMostSpecificCause().getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> invalidRequest(Exception ex) {
        log.error("Invalid or malformed request", ex);
        return respond(UniversalFault.BAD_REQUEST, null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unhandledError(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            // framework errors (404 no handler, 405, 415, …) keep their own status
            return respond(UniversalFault.BAD_REQUEST, ex.getMessage(), er.getStatusCode());
        }
        log.fatal("Uncaught exception during runtime", ex);
        return respond(UniversalFault.GENERAL_ERROR, null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
