package com.mobilise.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BorrowingEventDTO {
    private String isbn;
    private String bookTitle;
    private LocalDateTime borrowedAt;
    private LocalDateTime returnedAt;
}