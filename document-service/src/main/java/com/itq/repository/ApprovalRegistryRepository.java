package com.itq.repository;

import com.itq.entity.ApprovalRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {
    boolean existsByDocument_Id(Long documentId);
}
