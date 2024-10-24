package com.mobilise.mapper;

import com.mobilise.dto.BookDTO;
import com.mobilise.model.Book;

public class BookMapper {
    public static Book toEntity(BookDTO dto) {
        Book book = new Book();
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublicationYear(dto.getPublicationYear());
        book.setCopiesInStock(dto.getCopiesInStock());
        return book;
    }

//    public BookDTO toDTO(Book book) {
//        // Map fields from entity to DTO
//    }
//
//    public void updateBookFromDTO(BookDTO dto, Book book) {
//        // Update book fields from DTO
//    }
}
