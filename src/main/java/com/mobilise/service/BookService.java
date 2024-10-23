package com.mobilise.service;

import com.mobilise.constants.ResponseMessages;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService implements BookServiceInterface {
    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;

    @Transactional(readOnly = true)
    public ApiResponse<Page<Book>> getAllBooks(Pageable pageable) {
        log.debug("Fetching all books with pagination: {}", pageable);
        try {
            Page<Book> books = bookRepository.findAll(pageable);
            log.info("Successfully retrieved {} books", books.getTotalElements());
            return ApiResponse.success(books, ResponseMessages.BOOKS_RETRIEVED);
        } catch (Exception e) {
            log.error("Failed to retrieve books: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve books",
                    new ErrorDetails(ResponseMessages.FETCH_ERROR, e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<Book> getBookByIsbn(String isbn) {
        log.debug("Fetching book with ISBN: {}", isbn);
        try {
            Book book = bookRepository.findById(isbn)
                    .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));
            log.info("Successfully retrieved book with ISBN: {}", isbn);
            return ApiResponse.success(book, ResponseMessages.BOOK_RETRIEVED);
        } catch (BookNotFoundException e) {
            log.warn("Book not found with ISBN: {}", isbn);
            return ApiResponse.error("Book not found",
                    new ErrorDetails(ResponseMessages.NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving book with ISBN {}: {}", isbn, e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve book",
                    new ErrorDetails(ResponseMessages.FETCH_ERROR, e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Book> createBook(BookDTO bookDTO) {
        log.debug("Creating new book with ISBN: {}", bookDTO.getIsbn());
        try {
            Book book = new Book();
            updateBookFromDTO(book, bookDTO);
            Book savedBook = bookRepository.save(book);
            log.info("Successfully created book with ISBN: {}", savedBook.getIsbn());
            return ApiResponse.success(savedBook, ResponseMessages.BOOK_CREATED);
        } catch (Exception e) {
            log.error("Failed to create book: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to create book",
                    new ErrorDetails(ResponseMessages.CREATE_ERROR, e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Book> updateBook(String isbn, BookDTO bookDTO) {
        log.debug("Updating book with ISBN: {}", bookDTO.getIsbn());
        try {
            Book book = getBookByIsbn(isbn).getData();
            updateBookFromDTO(book, bookDTO);
            Book updatedBook = bookRepository.save(book);
            log.info("Successfully updated book with ISBN: {}", updatedBook.getIsbn());
            return ApiResponse.success(updatedBook, ResponseMessages.BOOK_UPDATED);
        } catch (BookNotFoundException e) {
            log.warn("Failed to update book: {}", e.getMessage(), e);
            return ApiResponse.error("Book not found",
                    new ErrorDetails("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update book: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to update book",
                    new ErrorDetails(ResponseMessages.UPDATE_ERROR, e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Void> deleteBook(String isbn) {
        log.debug("Deleting book with ISBN: {}", isbn);
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

            log.info("Successfully deleted book with ISBN: {}", isbn);
            return ApiResponse.success(null, ResponseMessages.BOOK_DELETED);
        } catch (BookNotFoundException e) {
            log.warn("Failed to delete book: {}", e.getMessage(), e);
            return ApiResponse.error("Book not found",
                    new ErrorDetails(ResponseMessages.NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete book: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to delete book",
                    new ErrorDetails(ResponseMessages.DELETE_ERROR, e.getMessage()));
        }
    }

    //Method to restore a deleted book
    public ApiResponse<Book> restoreBook(String isbn) {
        log.debug("Restoring book with ISBN: {}", isbn);
        try {
            Book book = bookRepository.findById(isbn)
                    .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));

            if (!book.isDeleted()) {
                throw new InvalidOperationException("Book is not deleted");
            }

            book.setDeleted(false);
            book.setDeletedAt(null);
            bookRepository.save(book);

            log.info("Successfully restored book with ISBN: {}", isbn);
            return ApiResponse.success(book, ResponseMessages.BOOK_RESTORED);
        } catch (BookNotFoundException e) {
            log.warn("Failed to restore book: {}", e.getMessage(), e);
            return ApiResponse.error("Book not found",
                    new ErrorDetails(ResponseMessages.NOT_FOUND, e.getMessage()));
        } catch (InvalidOperationException e) {
            log.warn("Failed to restore book: {}", e.getMessage(), e);
            return ApiResponse.error("Book not found",
                    new ErrorDetails(ResponseMessages.INVALID_OPERATION, e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to restore book: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to delete book",
                    new ErrorDetails(ResponseMessages.RESTORE_ERROR, e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<Book>> searchBooks(String query, Pageable pageable) {
        log.debug("Searching book with query: {}", query);

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

            log.info("Successfully searched book with query: {}", query);
            return ApiResponse.success(books, ResponseMessages.BOOKS_RETRIEVED);
        } catch (Exception e) {
            log.error("Failed to search book: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to search books",
                    new ErrorDetails(ResponseMessages.SEARCH_ERROR, e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<BorrowingRecord> borrowBook(String isbn) {
        log.debug("Attempting to borrow book with ISBN: {}", isbn);
        try {
            Book book = getBookByIsbn(isbn).getData();
            if (book.getCopiesInStock() <= 0) {
                log.warn("No copies available for book with ISBN: {}", isbn);
                throw new InvalidOperationException("No copies available for borrowing");
            }

            log.debug("Updating stock count for book: {}", isbn);
            book.setCopiesInStock(book.getCopiesInStock() - 1);
            bookRepository.save(book);

            log.debug("Creating borrowing record for book: {}", isbn);
            BorrowingRecord record = new BorrowingRecord();
            record.setBook(book);
            record.setBorrowedAt(LocalDateTime.now());
            BorrowingRecord savedRecord = borrowingRecordRepository.save(record);

            log.info("Successfully borrowed book with ISBN: {}", isbn);
            return ApiResponse.success(savedRecord, ResponseMessages.BOOK_BORROWED);
        } catch (InvalidOperationException e) {
            log.warn("Invalid operation while borrowing book {}: {}", isbn, e.getMessage());
            return ApiResponse.error("Invalid operation",
                    new ErrorDetails(ResponseMessages.INVALID_OPERATION, e.getMessage()));
        } catch (Exception e) {
            log.error("Error borrowing book {}: {}", isbn, e.getMessage(), e);
            return ApiResponse.error("Failed to borrow book",
                    new ErrorDetails(ResponseMessages.BORROW_ERROR, e.getMessage()));
        }
    }

    @Transactional
    public ApiResponse<Void> bulkUploadBooks(MultipartFile file) {
        log.debug("Attempting to upload file");
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

            log.info("Successfully uploaded file");
            return ApiResponse.success(null, ResponseMessages.BOOKS_UPLOADED);
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to process CSV file",
                    new ErrorDetails(ResponseMessages.UPLOAD_ERROR, e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<BorrowingReportDTO> generateBorrowingReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Attempting to generate borrowing report");
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

            log.info("Successfully generated borrowing report");
            return ApiResponse.success(report, ResponseMessages.REPORT_GENERATED);
        } catch (Exception e) {
            log.error("Error generating borrowing report {}", e.getMessage(), e);
            return ApiResponse.error("Failed to generate borrowing report",
                    new ErrorDetails(ResponseMessages.REPORT_ERROR, e.getMessage()));
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
        log.debug("Borrowing book with ISBN: {}", isbn);
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

            log.info("Successfully returned book with ISBN: {}", isbn);
            return ApiResponse.success(record, ResponseMessages.BOOK_RETURNED);
        } catch (BookNotFoundException e) {
            log.error("Error returning book {}: {}", isbn, e.getMessage(), e);
            return ApiResponse.error("Book not found",
                    new ErrorDetails("NOT_FOUND", e.getMessage()));
        } catch (InvalidOperationException e) {
            log.error("Error returning book {}: {}", isbn, e.getMessage(), e);
            return ApiResponse.error("Failed to return book",
                    new ErrorDetails(ResponseMessages.RETURN_ERROR, e.getMessage()));
        }
    }
}