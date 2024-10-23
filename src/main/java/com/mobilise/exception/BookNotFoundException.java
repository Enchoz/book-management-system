package com.mobilise.exception;

import com.mobilise.constants.ErrorCode;

public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}