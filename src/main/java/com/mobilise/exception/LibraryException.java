package com.mobilise.exception;

import com.mobilise.constants.ErrorCode;
import lombok.Getter;

@Getter
public class LibraryException extends RuntimeException {
    private final ErrorCode errorCode;

    public LibraryException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LibraryException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
}