package com.mobilise.repository;

import com.mobilise.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, String> {
    Page<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
            String title, String author, Pageable pageable);

    Optional<Book> findByIsbnAndDeletedIsFalse(String isbn);
}
