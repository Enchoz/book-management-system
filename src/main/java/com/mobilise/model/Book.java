package com.mobilise.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "books")
public class Book {
    @Id
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

    @Setter
    @Getter
    @Column(name = "is_deleted")
    private boolean deleted = false;

    @Setter
    @Getter
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}