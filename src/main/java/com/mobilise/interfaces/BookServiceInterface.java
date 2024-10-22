package com.mobilise.interfaces;

import com.mobilise.dto.BookDTO;
import com.mobilise.dto.BorrowingReportDTO;
import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public interface BookServiceInterface {
    Page<Book> getAllBooks(Pageable pageable);

    Book getBookByIsbn(String isbn);

    Book createBook(BookDTO bookDTO);

    Book updateBook(String isbn, BookDTO bookDTO);

    void deleteBook(String isbn);

    Page<Book> searchBooks(String query, Pageable pageable);

    BorrowingRecord borrowBook(String isbn);

    BorrowingRecord returnBook(String isbn);

    void bulkUploadBooks(MultipartFile file);

    BorrowingReportDTO generateBorrowingReport(LocalDateTime startDate, LocalDateTime endDate);
}
