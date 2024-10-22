package com.mobilise.repository;

import com.mobilise.model.Book;
import com.mobilise.model.BorrowingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {
    List<BorrowingRecord> findByBorrowedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT br.book.isbn as isbn, COUNT(br) as count FROM BorrowingRecord br " +
            "WHERE br.borrowedAt BETWEEN ?1 AND ?2 GROUP BY br.book.isbn")
    List<Object[]> countBorrowingsByBookAndDateRange(LocalDateTime start, LocalDateTime end);

    List<BorrowingRecord> findByBookAndReturnedAtIsNull(Book book);
}
