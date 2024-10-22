package com.mobilise.exception;

import com.mobilise.constants.ErrorCode;

public class BookDeleteException extends LibraryException {
    public BookDeleteException(String message) {
        super(ErrorCode.INVALID_OPERATION, message);
    }
}