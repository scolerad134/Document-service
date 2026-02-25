package com.itq.service;

import com.itq.entity.ApprovalRegistry;
import com.itq.entity.Document;
import com.itq.entity.History;
import com.itq.entity.enums.DocumentAction;
import com.itq.entity.enums.DocumentStatus;
import com.itq.exception.DocumentNotFoundException;
import com.itq.exception.RegistryException;
import com.itq.exception.ValidationException;
import com.itq.repository.DocumentRepository;
import com.itq.repository.HistoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ApproveOneExecutor {

    private final DocumentRepository documentRepository;
    private final HistoryRepository historyRepository;
    private final ApprovalRegistryWriter approvalRegistryWriter;

    public ApproveOneExecutor(DocumentRepository documentRepository,
                              HistoryRepository historyRepository,
                              ApprovalRegistryWriter approvalRegistryWriter) {
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.approvalRegistryWriter = approvalRegistryWriter;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Long id, String initiator, String comment) {
        Document doc = documentRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));

        if (doc.getStatus() != DocumentStatus.SUBMITTED) {
            throw new ValidationException("Document must be in SUBMITTED status to approve. Current: " + doc.getStatus());
        }

        doc.setStatus(DocumentStatus.APPROVED);
        documentRepository.save(doc);

        History history = new History();
        history.setDocument(doc);
        history.setAction(DocumentAction.APPROVE);
        history.setInitiator(initiator);
        history.setComment(comment);
        historyRepository.save(history);

        ApprovalRegistry registry = new ApprovalRegistry();
        registry.setDocument(doc);
        registry.setApprovedBy(initiator);
        try {
            approvalRegistryWriter.write(registry);
        } catch (Exception e) {
            throw new RegistryException("Failed to register approval", e);
        }
    }
}
