package com.mobilise.exception;

import com.mobilise.constants.ErrorCode;

public class InvalidOperationException extends LibraryException {
    public InvalidOperationException(String message) {
        super(ErrorCode.INVALID_OPERATION, message);
    }
}