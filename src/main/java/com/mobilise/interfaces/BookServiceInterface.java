package com.mobilise.interfaces;

import com.mobilise.dto.ApiResponse;
import com.mobilise.dto.BookDTO;
import com.mobilise.dto.BorrowingReportDTO;
import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public interface BookServiceInterface {
    ApiResponse<Page<Book>> getAllBooks(Pageable pageable);

    ApiResponse<Book> getBookByIsbn(String isbn);

    ApiResponse<Book> createBook(BookDTO bookDTO);

    ApiResponse<Book> updateBook(String isbn, BookDTO bookDTO);

    ApiResponse<Void> deleteBook(String isbn);

    ApiResponse<Page<Book>> searchBooks(String query, Pageable pageable);

    ApiResponse<BorrowingRecord> borrowBook(String isbn);

    ApiResponse<BorrowingRecord> returnBook(String isbn);

    ApiResponse<Void> bulkUploadBooks(MultipartFile file);

    ApiResponse<BorrowingReportDTO> generateBorrowingReport(LocalDateTime startDate, LocalDateTime endDate);

    ApiResponse<Book> restoreBook(String isbn);
}
