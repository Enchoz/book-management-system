package com.mobilise.service;

import com.mobilise.dto.*;
import com.mobilise.exception.BookDeleteException;
import com.mobilise.exception.BookNotFoundException;
import com.mobilise.exception.InvalidOperationException;
import com.mobilise.interfaces.BookServiceInterface;
import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import com.mobilise.repository.BookRepository;
import com.mobilise.repository.BorrowingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService implements BookServiceInterface {
    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;

    @Transactional(readOnly = true)
    public ApiResponse<Page<Book>> getAllBooks(Pageable pageable) {
        try {
            Page<Book> books = bookRepository.findAll(pageable);
            return ApiResponse.success(books, "Books retrieved successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve books",
                    new ErrorDetails("FETCH_ERROR", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<Book> getBookByIsbn(String isbn) {
        try {
            Book book = bookRepository.findById(isbn)
                    .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));
            return ApiResponse.success(book, "Book retrieved successfully");
        } catch (BookNotFoundException e) {
            return ApiResponse.error("Book not found",
                    new ErrorDetails("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve book",
                    new ErrorDetails("FETCH_ERROR", e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Book> createBook(BookDTO bookDTO) {
        try {
            Book book = new Book();
            updateBookFromDTO(book, bookDTO);
            Book savedBook = bookRepository.save(book);
            return ApiResponse.success(savedBook, "Book created successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to create book",
                    new ErrorDetails("CREATE_ERROR", e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Book> updateBook(String isbn, BookDTO bookDTO) {
        try {
            Book book = getBookByIsbn(isbn).getData();
            updateBookFromDTO(book, bookDTO);
            Book updatedBook = bookRepository.save(book);
            return ApiResponse.success(updatedBook, "Book updated successfully");
        } catch (BookNotFoundException e) {
            return ApiResponse.error("Book not found",
                    new ErrorDetails("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update book",
                    new ErrorDetails("UPDATE_ERROR", e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Void> deleteBook(String isbn) {
        try {
            Book book = bookRepository.findByIsbnAndDeletedIsFalse(isbn)
                    .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));

            // Check if book has any active borrowings
            List<BorrowingRecord> activeBorrowings = borrowingRecordRepository
                    .findByBookAndReturnedAtIsNull(book);

            if (!activeBorrowings.isEmpty()) {
                throw new BookDeleteException(
                        "Cannot delete book as it is currently borrowed. Active borrowings: " +
                                activeBorrowings.size());
            }

            // Perform soft delete
            book.setDeleted(true);
            book.setDeletedAt(LocalDateTime.now());
                bookRepository.save(book);

            return ApiResponse.success(null, "Book successfully marked as deleted");
        } catch (BookNotFoundException e) {
            return ApiResponse.error("Book not found",
                    new ErrorDetails("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete book",
                    new ErrorDetails("DELETE_ERROR", e.getMessage()));
        }
    }

    //Method to restore a deleted book
    public ApiResponse<Book> restoreBook(String isbn) {
        try {
            Book book = bookRepository.findById(isbn)
                    .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));

            if (!book.isDeleted()) {
                throw new InvalidOperationException("Book is not deleted");
            }

            book.setDeleted(false);
            book.setDeletedAt(null);
            bookRepository.save(book);
            return ApiResponse.success(book, "Book successfully restored");
        } catch (BookNotFoundException e) {
            return ApiResponse.error("Book not found",
                    new ErrorDetails("NOT_FOUND", e.getMessage()));
        } catch (InvalidOperationException e) {
            return ApiResponse.error("Book not found",
                    new ErrorDetails("INVALID_OPERATION", e.getMessage()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete book",
                    new ErrorDetails("RESTORE_ERROR", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<Book>> searchBooks(String query, Pageable pageable) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ApiResponse.error("Search query cannot be empty",
                        new ErrorDetails("INVALID_QUERY", "Search query must not be empty"));
            }

            Page<Book> books = bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                    query.trim(), query.trim(), pageable);

            if (books.isEmpty()) {
                return ApiResponse.success(books, "No books found matching the search criteria");
            }

            return ApiResponse.success(books, "Books found successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to search books",
                    new ErrorDetails("SEARCH_ERROR", e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<BorrowingRecord> borrowBook(String isbn) {
        try {
            Book book = getBookByIsbn(isbn).getData();
            if (book.getCopiesInStock() <= 0) {
                throw new InvalidOperationException("No copies available for borrowing");
            }

            book.setCopiesInStock(book.getCopiesInStock() - 1);
            bookRepository.save(book);

            BorrowingRecord record = new BorrowingRecord();
            record.setBook(book);
            record.setBorrowedAt(LocalDateTime.now());
            BorrowingRecord savedRecord = borrowingRecordRepository.save(record);

            return ApiResponse.success(savedRecord, "Book borrowed successfully");
        } catch (InvalidOperationException e) {
            return ApiResponse.error("Invalid operation",
                    new ErrorDetails("INVALID_OPERATION", e.getMessage()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to borrow book",
                    new ErrorDetails("BORROW_ERROR", e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Void> bulkUploadBooks(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

            for (CSVRecord record : csvParser) {
                BookDTO bookDTO = new BookDTO();
                bookDTO.setIsbn(record.get("ISBN"));
                bookDTO.setTitle(record.get("title"));
                bookDTO.setAuthor(record.get("author"));
                bookDTO.setPublicationYear(Integer.parseInt(record.get("publication_year")));
                bookDTO.setCopiesInStock(Integer.parseInt(record.get("copies")));

                createBook(bookDTO);
            }
            return ApiResponse.success(null, "Books uploaded successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to process CSV file",
                    new ErrorDetails("UPLOAD_ERROR", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<BorrowingReportDTO> generateBorrowingReport(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<BorrowingRecord> borrowings = borrowingRecordRepository
                    .findByBorrowedAtBetween(startDate, endDate);

            if (borrowings == null || borrowings.isEmpty()) {
                return ApiResponse.success(createEmptyBorrowingReport(), "No borrowing records found for the period");
            }

            Map<String, Long> borrowingCounts = borrowingRecordRepository
                    .countBorrowingsByBookAndDateRange(startDate, endDate)
                    .stream()
                    .collect(Collectors.toMap(
                            row -> (String) row[0],
                            row -> (Long) row[1]
                    ));

            List<BorrowingEventDTO> events = borrowings.stream()
                    .map(this::convertToBorrowingEventDTO)
                    .collect(Collectors.toList());

            BorrowingReportDTO report = new BorrowingReportDTO();
            report.setBorrowingCountsByBook(borrowingCounts);
            report.setBorrowingEvents(events);

            return ApiResponse.success(report, "Borrowing report generated successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to generate borrowing report",
                    new ErrorDetails("REPORT_ERROR", e.getMessage()));
        }
    }
    private BorrowingReportDTO createEmptyBorrowingReport() {
        BorrowingReportDTO emptyReport = new BorrowingReportDTO();
        emptyReport.setBorrowingCountsByBook(Collections.emptyMap());
        emptyReport.setBorrowingEvents(Collections.emptyList());
        return emptyReport;
    }

    private void updateBookFromDTO(Book book, BookDTO dto) {
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublicationYear(dto.getPublicationYear());
        book.setCopiesInStock(dto.getCopiesInStock());
    }

    private BorrowingEventDTO convertToBorrowingEventDTO(BorrowingRecord record) {
        BorrowingEventDTO dto = new BorrowingEventDTO();
        dto.setIsbn(record.getBook().getIsbn());
        dto.setBookTitle(record.getBook().getTitle());
        dto.setBorrowedAt(record.getBorrowedAt());
        dto.setReturnedAt(record.getReturnedAt());
        return dto;
    }

    @Transactional
    public ApiResponse<BorrowingRecord> returnBook(String isbn) {
        try {
            Book book = bookRepository.findById(isbn)
                    .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + isbn));

            // Find the most recent unreturned borrowing record for this book
            BorrowingRecord record = borrowingRecordRepository.findAll().stream()
                    .filter(br -> br.getBook().getIsbn().equals(isbn))
                    .filter(br -> br.getReturnedAt() == null)
                    .findFirst()
                    .orElseThrow(() -> new InvalidOperationException("No active borrowing record found for this book"));

            log.debug("Updating return date and stock count for book: {}", isbn);
            record.setReturnedAt(LocalDateTime.now());
            book.setCopiesInStock(book.getCopiesInStock() + 1);
            bookRepository.save(book);

            borrowingRecordRepository.save(record);
            return ApiResponse.success(record, "Book returned successfully");
        } catch (BookNotFoundException e) {
            return ApiResponse.error("Book not found",
                    new ErrorDetails("NOT_FOUND", e.getMessage()));
        } catch (InvalidOperationException e) {
            return ApiResponse.error("Failed to return book",
                    new ErrorDetails("RETURN_ERROR", e.getMessage()));
        }
    }
}