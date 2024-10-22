package com.mobilise.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class BookDTO {
    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    @NotNull(message = "Publication year is required")
    @Min(value = 1000, message = "Publication year must be after 1000")
    @Max(value = 9999, message = "Publication year must be before 9999")
    private Integer publicationYear;

    @Min(value = 0, message = "Number of copies cannot be negative")
    private Integer copiesInStock;
}
