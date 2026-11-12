package com.fajekrina.universal.utils.exception;

import com.fajekrina.universal.utils.enums.UniversalFault;
import lombok.Getter;

/** Thrown by services; rendered by GlobalErrorHandler from the fault (status + code) and the detail. */
@Getter
public class UniversalException extends RuntimeException {

    private final transient UniversalFault fault;

    public UniversalException(UniversalFault fault) {
        super(fault.getMessage());
        this.fault = fault;
    }

    /** @param detail human-readable detail shown as errorMessage (e.g. which validation rule failed) */
    public UniversalException(UniversalFault fault, String detail) {
        super(detail == null ? fault.getMessage() : detail);
        this.fault = fault;
    }

    public UniversalException(UniversalFault fault, String detail, Throwable cause) {
        super(detail == null ? fault.getMessage() : detail, cause);
        this.fault = fault;
    }
}
