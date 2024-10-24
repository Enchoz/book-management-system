package com.mobilise.service;

import com.mobilise.constants.ResponseMessages;
import com.mobilise.dto.ApiResponse;
import com.mobilise.dto.BookDTO;
import com.mobilise.dto.BorrowingReportDTO;
import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import com.mobilise.repository.BookRepository;
import com.mobilise.repository.BorrowingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowingRecordRepository borrowingRecordRepository;

    @Spy
    @InjectMocks
    private BookService bookService;

    private Book testBook;
    private BookDTO testBookDTO;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setIsbn("1234567890");
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setPublicationYear(2023);
        testBook.setCopiesInStock(5);

        testBookDTO = new BookDTO();
        testBookDTO.setIsbn("1234567890");
        testBookDTO.setTitle("Test Book");
        testBookDTO.setAuthor("Test Author");
        testBookDTO.setPublicationYear(2023);
        testBookDTO.setCopiesInStock(5);
    }

    @Test
    void getAllBooks_WhenSuccessful_ShouldReturnSuccessResponse() {
        Page<Book> bookPage = new PageImpl<>(List.of(testBook));
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);

        ApiResponse<Page<Book>> response = bookService.getAllBooks(Pageable.unpaged());

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getTotalElements());
        assertNull(response.getError());
        verify(bookRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllBooks_WhenError_ShouldReturnErrorResponse() {
        when(bookRepository.findAll(any(Pageable.class))).thenThrow(new RuntimeException("Database error"));

        ApiResponse<Page<Book>> response = bookService.getAllBooks(Pageable.unpaged());

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertEquals("FETCH_ERROR", response.getError().getCode());
        assertNull(response.getData());
    }

    @Test
    void getBookByIsbn_WhenExists_ShouldReturnSuccessResponse() {
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));

        ApiResponse<Book> response = bookService.getBookByIsbn(testBook.getIsbn());

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(testBook.getIsbn(), response.getData().getIsbn());
        assertNull(response.getError());
    }

    @Test
    void getBookByIsbn_WhenNotExists_ShouldReturnErrorResponse() {
        when(bookRepository.findById(anyString())).thenReturn(Optional.empty());

        ApiResponse<Book> response = bookService.getBookByIsbn("nonexistent");

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertEquals("NOT_FOUND", response.getError().getCode());
        assertNull(response.getData());
    }

    @Test
    void borrowBook_WhenNoCopiesAvailable_ShouldReturnErrorResponse() {
        testBook.setCopiesInStock(0);
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));

        ApiResponse<BorrowingRecord> response = bookService.borrowBook(testBook.getIsbn());

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertEquals("INVALID_OPERATION", response.getError().getCode());
        assertNull(response.getData());
    }

    @Test
    void borrowBook_WhenCopiesAvailable_ShouldReturnSuccessResponse() {
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));
        when(borrowingRecordRepository.save(any(BorrowingRecord.class)))
                .thenReturn(new BorrowingRecord());

        int initialCopiesOfBook = testBook.getCopiesInStock();

        ApiResponse<BorrowingRecord> response = bookService.borrowBook(testBook.getIsbn());

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNull(response.getError());
        assertEquals(initialCopiesOfBook - 1, testBook.getCopiesInStock());
    }

    // Add these test methods to BookServiceTest class

    @Test
    void searchBooks_WhenQueryValid_ShouldReturnSuccessResponse() {
        Page<Book> bookPage = new PageImpl<>(List.of(testBook));
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                anyString(), anyString(), any(Pageable.class)))
                .thenReturn(bookPage);

        ApiResponse<Page<Book>> response = bookService.searchBooks("Test", Pageable.unpaged());

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getTotalElements());
        assertNull(response.getError());
        assertEquals(ResponseMessages.BOOKS_RETRIEVED, response.getMessage());
    }

    @Test
    void searchBooks_WhenEmptyQuery_ShouldReturnErrorResponse() {
        ApiResponse<Page<Book>> response = bookService.searchBooks("  ", Pageable.unpaged());

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertEquals("INVALID_QUERY", response.getError().getCode());
        assertNull(response.getData());
    }

    @Test
    void updateBook_WhenSuccessful_ShouldReturnUpdatedBook() {
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        testBookDTO.setTitle("Updated Title");
        ApiResponse<Book> response = bookService.updateBook(testBook.getIsbn(), testBookDTO);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Updated Title", response.getData().getTitle());
        assertEquals(ResponseMessages.BOOK_UPDATED, response.getMessage());
    }

    @Test
    void deleteBook_WhenSuccessful_ShouldReturnSuccessResponse() {
        // Create a test book
        Book testBook = new Book();
        testBook.setIsbn("1234567890");
        testBook.setDeleted(false);  // Ensure the book is not marked as deleted initially

        // Mock the book repository to find the book and ensure it's not deleted
        when(bookRepository.findByIsbnAndDeletedIsFalse(testBook.getIsbn())).thenReturn(Optional.of(testBook));

        // Mock the borrowing record repository to return no active borrowings (book is not currently borrowed)
        when(borrowingRecordRepository.findByBookAndReturnedAtIsNull(testBook)).thenReturn(List.of());

        // Mock the repository save method to perform the soft delete
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // Call the service method to delete the book
        ApiResponse<Void> response = bookService.deleteBook(testBook.getIsbn());

        // Assertions
        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertEquals(ResponseMessages.BOOK_DELETED, response.getMessage());

        // Verify interactions with the repositories
        verify(bookRepository, times(1)).findByIsbnAndDeletedIsFalse(testBook.getIsbn());
        verify(borrowingRecordRepository, times(1)).findByBookAndReturnedAtIsNull(testBook);
        verify(bookRepository, times(1)).save(any(Book.class));
    }


    @Test
    void returnBook_WhenSuccessful_ShouldReturnSuccessResponse() {
        // Create a borrowing record where the book was borrowed and not yet returned
        BorrowingRecord borrowingRecord = new BorrowingRecord();
        borrowingRecord.setBook(testBook);
        borrowingRecord.setBorrowedAt(LocalDateTime.now());
        borrowingRecord.setReturnedAt(null);  // The book is not yet returned

        // Mock the book repository to return the test book when the ISBN is searched
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));

        // Mock the borrowing record repository to return the unreturned record for this book
        when(borrowingRecordRepository.findFirstByBookAndReturnedAtIsNull(testBook))
                .thenReturn(Optional.of(borrowingRecord));

        // Mock the repository save method to return the updated borrowing record
        when(borrowingRecordRepository.save(any(BorrowingRecord.class))).thenReturn(borrowingRecord);

        // Call the service method to return the book
        ApiResponse<BorrowingRecord> response = bookService.returnBook(testBook.getIsbn());

        // Assertions
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getReturnedAt());  // Ensure the book is marked as returned
        assertEquals(ResponseMessages.BOOK_RETURNED, response.getMessage());

        // Verify interactions with the repositories
        verify(bookRepository, times(1)).findById(testBook.getIsbn());
        verify(borrowingRecordRepository, times(1)).findFirstByBookAndReturnedAtIsNull(testBook);
        verify(borrowingRecordRepository, times(1)).save(any(BorrowingRecord.class));
    }

    @Test
    void returnBook_WhenNoBorrowingRecord_ShouldReturnErrorResponse() {
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));
        when(borrowingRecordRepository.findFirstByBookAndReturnedAtIsNull(testBook))
                .thenReturn(Optional.empty());

        ApiResponse<BorrowingRecord> response = bookService.returnBook(testBook.getIsbn());

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertEquals("RETURN_ERROR", response.getError().getCode());
    }

    @Test
    void generateBorrowingReport_WhenDataExists_ShouldReturnSuccessResponse() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        BorrowingRecord record = new BorrowingRecord();
        record.setBook(testBook);
        record.setBorrowedAt(LocalDateTime.now().minusDays(5));

        List<Object[]> countData = List.of(new Object[][] {
                new Object[]{testBook.getIsbn(), 1L}
        });

        when(borrowingRecordRepository.findByBorrowedAtBetween(startDate, endDate))
                .thenReturn(List.of(record));
        when(borrowingRecordRepository.countBorrowingsByBookAndDateRange(startDate, endDate))
                .thenReturn(countData);

        ApiResponse<BorrowingReportDTO> response = bookService.generateBorrowingReport(startDate, endDate);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(ResponseMessages.REPORT_GENERATED, response.getMessage());
    }

    @Test
    void bulkUploadBooks_WhenSuccessful_ShouldReturnSuccessResponse() throws IOException {
        // Mocking the MultipartFile and its input stream
        MultipartFile file = mock(MultipartFile.class);
        String csvContent = "ISBN,title,author,publication_year,copies\n123,Test,Author,2023,5";

        // Use a ByteArrayInputStream to simulate reading from a file
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        when(file.getInputStream()).thenReturn(inputStream);

        // Mock the repository to simulate saving books
        List<Book> mockBooks = new ArrayList<>();
        Book testBook = new Book("123", "Test", "Author", 2023, 5, false, null);
        mockBooks.add(testBook);

        // Mock the repository method saveAll
        when(bookRepository.saveAll(anyList())).thenReturn(mockBooks);

        // Call the service method
        ApiResponse<Void> response = bookService.bulkUploadBooks(file);

        // Assertions
        assertTrue(response.isSuccess());
        assertEquals(ResponseMessages.BOOKS_UPLOADED, response.getMessage());

        // Verify repository interaction
        verify(bookRepository, times(1)).saveAll(anyList());
    }
}