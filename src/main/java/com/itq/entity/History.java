package com.itq.entity;

import com.itq.entity.enums.DocumentAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "history", indexes = @Index(name="ix_hist_doc_time", columnList="document_id, created_at"))
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentAction action;

    @Column(name = "initiator", nullable = false)
    private String initiator;

    @Column(name = "comment", nullable = false)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
