package com.mobilise.controller;

import com.mobilise.dto.ApiResponse;
import com.mobilise.dto.BookDTO;
import com.mobilise.dto.BorrowingReportDTO;
import com.mobilise.interfaces.BookServiceInterface;
import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Book Management API", description = "APIs for managing books in the library")
@Validated
public class BookController {
    private final BookServiceInterface bookService;

    @GetMapping
    @Operation(
            summary = "Get all books with pagination",
            description = "Retrieves a paginated list of all books in the library"
    )
    public ResponseEntity<ApiResponse<Page<Book>>> getAllBooks(
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        ApiResponse<Page<Book>> response = bookService.getAllBooks(pageable);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @GetMapping("/{isbn}")
    @Operation(
            summary = "Get a book by ISBN",
            description = "Retrieves a specific book using its ISBN"
    )
    public ResponseEntity<ApiResponse<Book>> getBookByIsbn(
            @Parameter(description = "ISBN of the book", required = true)
            @PathVariable String isbn) {
        ApiResponse<Book> response = bookService.getBookByIsbn(isbn);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @PostMapping
    @Operation(
            summary = "Create a new book",
            description = "Adds a new book to the library"
    )
    public ResponseEntity<ApiResponse<Book>> createBook(
            @Parameter(description = "Book details", required = true)
            @Valid @RequestBody BookDTO bookDTO) {
        ApiResponse<Book> response = bookService.createBook(bookDTO);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PutMapping("/{isbn}")
    @Operation(
            summary = "Update an existing book",
            description = "Updates the details of an existing book"
    )
    public ResponseEntity<ApiResponse<Book>> updateBook(
            @Parameter(description = "ISBN of the book to update", required = true)
            @PathVariable String isbn,
            @Parameter(description = "Updated book details", required = true)
            @Valid @RequestBody BookDTO bookDTO) {
        ApiResponse<Book> response = bookService.updateBook(isbn, bookDTO);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @DeleteMapping("/{isbn}")
    @Operation(
            summary = "Delete a book",
            description = "Removes a book from the library"
    )
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @Parameter(description = "ISBN of the book to delete", required = true)
            @PathVariable String isbn) {
        ApiResponse<Void> response = bookService.deleteBook(isbn);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search books by title or author",
            description = "Searches for books based on title or author name"
    )
    public ResponseEntity<ApiResponse<Page<Book>>> searchBooks(
            @Parameter(description = "Search query", required = true)
            @RequestParam @NotBlank(message = "Search query cannot be empty") String query,
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        ApiResponse<Page<Book>> response = bookService.searchBooks(query, pageable);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping("/{isbn}/borrow")
    @Operation(
            summary = "Borrow a book",
            description = "Records a book borrowing transaction"
    )
    public ResponseEntity<ApiResponse<BorrowingRecord>> borrowBook(
            @Parameter(description = "ISBN of the book to borrow", required = true)
            @PathVariable String isbn) {
        ApiResponse<BorrowingRecord> response = bookService.borrowBook(isbn);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping("/{isbn}/return")
    @Operation(
            summary = "Return a borrowed book",
            description = "Records a book return transaction"
    )
    public ResponseEntity<ApiResponse<BorrowingRecord>> returnBook(
            @Parameter(description = "ISBN of the book to return", required = true)
            @PathVariable String isbn) {
        ApiResponse<BorrowingRecord> response = bookService.returnBook(isbn);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping("/bulk-upload")
    @Operation(
            summary = "Bulk upload books from CSV file",
            description = "Imports multiple books from a CSV file"
    )
    public ResponseEntity<ApiResponse<Void>> bulkUploadBooks(
            @Parameter(description = "CSV file containing book details", required = true)
            @RequestParam("file") MultipartFile file) {
        ApiResponse<Void> response = bookService.bulkUploadBooks(file);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/borrowing-report")
    @Operation(
            summary = "Generate borrowing report for a date range",
            description = "Creates a report of all borrowing activities within the specified date range"
    )
    public ResponseEntity<ApiResponse<BorrowingReportDTO>> generateBorrowingReport(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        ApiResponse<BorrowingReportDTO> response = bookService.generateBorrowingReport(startDate, endDate);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
}