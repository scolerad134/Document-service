package com.itq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "approval_registry", uniqueConstraints = @UniqueConstraint(name="uk_registry_doc", columnNames="document_id"))
public class ApprovalRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "document_id", referencedColumnName = "id")
    private Document document;

    @Column(name = "approved_by", nullable = false)
    private String approved_by;

    @Column(name = "approved_at")
    private LocalDateTime approved_at;

    @PrePersist
    protected void onCreate() {
        approved_at = LocalDateTime.now();
    }
}
