package com.mobilise.service;

import com.mobilise.dto.BookDTO;
import com.mobilise.exception.BookNotFoundException;
import com.mobilise.exception.InvalidOperationException;
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

import java.io.IOException;
import java.time.LocalDateTime;
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
    void getAllBooks_ShouldReturnPageOfBooks() {
        Page<Book> bookPage = new PageImpl<>(List.of(testBook));
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);

        Page<Book> result = bookService.getAllBooks(Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(bookRepository).findAll(any(Pageable.class));
    }

    @Test
    void getBookByIsbn_WhenExists_ShouldReturnBook() {
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));

        Book result = bookService.getBookByIsbn(testBook.getIsbn());

        assertNotNull(result);
        assertEquals(testBook.getIsbn(), result.getIsbn());
        verify(bookRepository).findById(testBook.getIsbn());
    }

    @Test
    void getBookByIsbn_WhenNotExists_ShouldThrowException() {
        when(bookRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () ->
                bookService.getBookByIsbn("nonexistent"));
        verify(bookRepository).findById(anyString());
    }

    @Test
    void createBook_ShouldSaveAndReturnBook() {
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        Book result = bookService.createBook(testBookDTO);

        assertNotNull(result);
        assertEquals(testBook.getIsbn(), result.getIsbn());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void borrowBook_WhenNoCopiesAvailable_ShouldThrowException() {
        testBook.setCopiesInStock(0);
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));

        assertThrows(InvalidOperationException.class, () ->
                bookService.borrowBook(testBook.getIsbn()));
        verify(bookRepository).findById(testBook.getIsbn());
        verify(borrowingRecordRepository, never()).save(any());
    }

    @Test
    void updateBook_ShouldUpdateAndReturnBook() {
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        Book updatedBook = bookService.updateBook(testBook.getIsbn(), testBookDTO);

        assertNotNull(updatedBook);
        assertEquals(testBookDTO.getTitle(), updatedBook.getTitle());
        verify(bookRepository).findById(testBook.getIsbn());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void deleteBook_WhenBookExists_ShouldDelete() {
        when(bookRepository.existsById(testBook.getIsbn())).thenReturn(true);

        bookService.deleteBook(testBook.getIsbn());

        verify(bookRepository).existsById(testBook.getIsbn());
        verify(bookRepository).deleteById(testBook.getIsbn());
    }

    @Test
    void deleteBook_WhenBookDoesNotExist_ShouldThrowException() {
        when(bookRepository.existsById(testBook.getIsbn())).thenReturn(false);

        assertThrows(BookNotFoundException.class, () -> bookService.deleteBook(testBook.getIsbn()));
        verify(bookRepository).existsById(testBook.getIsbn());
        verify(bookRepository, never()).deleteById(anyString());
    }

    @Test
    void searchBooks_ShouldReturnMatchingBooks() {
        Page<Book> bookPage = new PageImpl<>(List.of(testBook));
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                anyString(), anyString(), any(Pageable.class)))
                .thenReturn(bookPage);

        Page<Book> result = bookService.searchBooks("Test", Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(bookRepository).findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void borrowBook_WhenCopiesAvailable_ShouldDecreaseCopies() {
        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));
        when(borrowingRecordRepository.save(any(BorrowingRecord.class))).thenReturn(new BorrowingRecord());

        // Store the initial number of copies in stock
        int initialCopiesInStock = testBook.getCopiesInStock();

        BorrowingRecord result = bookService.borrowBook(testBook.getIsbn());

        assertNotNull(result);
        assertEquals(initialCopiesInStock - 1, testBook.getCopiesInStock());
        verify(bookRepository).findById(testBook.getIsbn());
        verify(bookRepository).save(testBook);
        verify(borrowingRecordRepository).save(any(BorrowingRecord.class));
    }

    @Test
    void returnBook_WhenBookIsBorrowed_ShouldIncreaseCopies() {
        BorrowingRecord borrowingRecord = new BorrowingRecord();
        borrowingRecord.setBook(testBook);
        borrowingRecord.setBorrowedAt(LocalDateTime.now());

        when(bookRepository.findById(testBook.getIsbn())).thenReturn(Optional.of(testBook));
        when(borrowingRecordRepository.findAll()).thenReturn(List.of(borrowingRecord));
        when(borrowingRecordRepository.save(any(BorrowingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Store the initial number of copies in stock
        int initialCopiesInStock = testBook.getCopiesInStock();

        BorrowingRecord returnedRecord = bookService.returnBook(testBook.getIsbn());

        assertNotNull(returnedRecord.getReturnedAt());
        assertEquals(initialCopiesInStock + 1, testBook.getCopiesInStock());
        verify(bookRepository).save(testBook);
        verify(borrowingRecordRepository).save(borrowingRecord);
    }

    @Test
    void bulkUploadBooks_WhenFileIsInvalid_ShouldThrowException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new RuntimeException("File error"));

        assertThrows(InvalidOperationException.class, () -> bookService.bulkUploadBooks(file));
        verify(bookRepository, never()).save(any(Book.class));
    }

}