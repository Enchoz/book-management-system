package com.mobilise.service;

import com.mobilise.dto.BookDTO;
import com.mobilise.dto.BorrowingEventDTO;
import com.mobilise.dto.BorrowingReportDTO;
import com.mobilise.exception.BookNotFoundException;
import com.mobilise.exception.InvalidOperationException;
import com.mobilise.interfaces.BookServiceInterface;
import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import com.mobilise.repository.BookRepository;
import com.mobilise.repository.BorrowingRecordRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class BookService implements BookServiceInterface {
    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRecordRepository;

    @Transactional(readOnly = true)
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Book getBookByIsbn(String isbn) {
        return bookRepository.findById(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));
    }

    @Transactional
    public Book createBook(BookDTO bookDTO) {
        Book book = new Book();
        updateBookFromDTO(book, bookDTO);
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(String isbn, BookDTO bookDTO) {
        Book book = getBookByIsbn(isbn);
        updateBookFromDTO(book, bookDTO);
        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(String isbn) {
        if (!bookRepository.existsById(isbn)) {
            throw new BookNotFoundException("Book not found with ISBN: " + isbn);
        }
        bookRepository.deleteById(isbn);
    }

    @Transactional(readOnly = true)
    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                query, query, pageable);
    }

    @Transactional
    public BorrowingRecord borrowBook(String isbn) {
        Book book = getBookByIsbn(isbn);
        if (book.getCopiesInStock() <= 0) {
            throw new InvalidOperationException("No copies available for borrowing");
        }

        book.setCopiesInStock(book.getCopiesInStock() - 1);
        bookRepository.save(book);

        BorrowingRecord record = new BorrowingRecord();
        record.setBook(book);
        record.setBorrowedAt(LocalDateTime.now());
        return borrowingRecordRepository.save(record);
    }

    @Transactional
    public void bulkUploadBooks(MultipartFile file) {
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
        } catch (Exception e) {
            throw new InvalidOperationException("Error processing CSV file: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public BorrowingReportDTO generateBorrowingReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<BorrowingRecord> borrowings = borrowingRecordRepository
                .findByBorrowedAtBetween(startDate, endDate);

        if (borrowings == null || borrowings.isEmpty()) {
            return createEmptyBorrowingReport();
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
        return report;
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
    public BorrowingRecord returnBook(String isbn) {
        Book book = getBookByIsbn(isbn);

        // Find the most recent unreturned borrowing record for this book
        BorrowingRecord record = borrowingRecordRepository.findAll().stream()
                .filter(br -> br.getBook().getIsbn().equals(isbn))
                .filter(br -> br.getReturnedAt() == null)
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("No active borrowing record found for this book"));

        record.setReturnedAt(LocalDateTime.now());
        book.setCopiesInStock(book.getCopiesInStock() + 1);
        bookRepository.save(book);

        return borrowingRecordRepository.save(record);
    }
}