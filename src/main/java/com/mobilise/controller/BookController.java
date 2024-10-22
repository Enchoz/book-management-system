package com.mobilise.controller;

import com.mobilise.dto.BookDTO;
import com.mobilise.dto.BorrowingReportDTO;
import com.mobilise.interfaces.BookServiceInterface;
import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import com.mobilise.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Book Management API", description = "APIs for managing books in the library")
public class BookController {
    private final BookServiceInterface bookService;

    @GetMapping
    @Operation(summary = "Get all books with pagination")
    public ResponseEntity<Page<Book>> getAllBooks(Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{isbn}")
    @Operation(summary = "Get a book by ISBN")
    public ResponseEntity<Book> getBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
    }

    @PostMapping
    @Operation(summary = "Create a new book")
    public ResponseEntity<Book> createBook(@Valid @RequestBody BookDTO bookDTO) {
        return new ResponseEntity<>(bookService.createBook(bookDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{isbn}")
    @Operation(summary = "Update an existing book")
    public ResponseEntity<Book> updateBook(
            @PathVariable String isbn,
            @Valid @RequestBody BookDTO bookDTO) {
        return ResponseEntity.ok(bookService.updateBook(isbn, bookDTO));
    }

    @DeleteMapping("/{isbn}")
    @Operation(summary = "Delete a book")
    public ResponseEntity<Void> deleteBook(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search books by title or author")
    public ResponseEntity<Page<Book>> searchBooks(
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(bookService.searchBooks(query, pageable));
    }

    @PostMapping("/{isbn}/borrow")
    @Operation(summary = "Borrow a book")
    public ResponseEntity<BorrowingRecord> borrowBook(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.borrowBook(isbn));
    }

    @PostMapping("/{isbn}/return")
    @Operation(summary = "Return a borrowed book")
    public ResponseEntity<BorrowingRecord> returnBook(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.returnBook(isbn));
    }

    @PostMapping("/bulk-upload")
    @Operation(summary = "Bulk upload books from CSV file")
    public ResponseEntity<Void> bulkUploadBooks(@RequestParam("file") MultipartFile file) {
        bookService.bulkUploadBooks(file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/borrowing-report")
    @Operation(summary = "Generate borrowing report for a date range")
    public ResponseEntity<BorrowingReportDTO> generateBorrowingReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(bookService.generateBorrowingReport(startDate, endDate));
    }
}