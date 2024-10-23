package com.mobilise.constants;

public enum ErrorCode {
    NOT_FOUND("Resource not found"),
    FETCH_ERROR("Error fetching resource"),
    CREATE_ERROR("Error creating resource"),
    UPDATE_ERROR("Error updating resource"),
    DELETE_ERROR("Error deleting resource"),
    INVALID_OPERATION("Invalid operation"),
    INVALID_QUERY("Invalid query"),
    BORROW_ERROR("Error borrowing book"),
    RETURN_ERROR("Error returning book"),
    UPLOAD_ERROR("Error uploading books"),
    REPORT_ERROR("Error generating report");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}