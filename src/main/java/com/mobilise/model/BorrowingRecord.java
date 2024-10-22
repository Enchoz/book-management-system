package com.mobilise.model;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "borrowing_records")
public class BorrowingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "isbn", nullable = false)
    private Book book;

    @Column(nullable = false)
    private LocalDateTime borrowedAt;

    @Column
    private LocalDateTime returnedAt;
}