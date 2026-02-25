package com.itq.repository;

import com.itq.entity.Document;
import com.itq.entity.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByIdIn(List<Long> ids);

    long countByStatus(DocumentStatus status);

    @Query("""
        select d from Document d where
        (:status is null or d.status = :status) and
        (:author is null or d.author = :author) and
        (:fromDate is null or d.createdAt >= :fromDate) and
        (:toDate is null or d.createdAt <= :toDate)
        """)
    Page<Document> search(
        @Param("status") DocumentStatus status,
        @Param("author") String author,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );

    List<Document> findByStatusOrderByIdAsc(DocumentStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Document d where d.id = :id")
    java.util.Optional<Document> findByIdForUpdate(@Param("id") Long id);
}
