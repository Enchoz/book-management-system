package com.mobilise.constants;

public class ResponseMessages {
        // Error Codes
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String FETCH_ERROR = "FETCH_ERROR";
        public static final String CREATE_ERROR = "CREATE_ERROR";
        public static final String UPDATE_ERROR = "UPDATE_ERROR";
        public static final String DELETE_ERROR = "DELETE_ERROR";
        public static final String RESTORE_ERROR = "RESTORE_ERROR";
        public static final String INVALID_OPERATION = "INVALID_OPERATION";
        public static final String SEARCH_ERROR = "SEARCH_ERROR";
        public static final String BORROW_ERROR = "BORROW_ERROR";
        public static final String RETURN_ERROR = "RETURN_ERROR";
        public static final String UPLOAD_ERROR = "UPLOAD_ERROR";
        public static final String REPORT_ERROR = "REPORT_ERROR";

        // Success Messages
        public static final String BOOKS_RETRIEVED = "Books retrieved successfully";
        public static final String BOOK_RETRIEVED = "Book retrieved successfully";
        public static final String BOOK_CREATED = "Book created successfully";
        public static final String BOOK_UPDATED = "Book updated successfully";
        public static final String BOOK_DELETED = "Book successfully marked as deleted";
        public static final String BOOK_RESTORED = "Book successfully restored";
        public static final String BOOK_BORROWED = "Book borrowed successfully";
        public static final String BOOK_RETURNED = "Book returned successfully";
        public static final String BOOKS_UPLOADED = "Books uploaded successfully";
        public static final String REPORT_GENERATED = "Borrowing report generated successfully";

        private ResponseMessages() {} // Prevent instantiation
}