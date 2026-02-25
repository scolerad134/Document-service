package com.itq.service;

import com.itq.dto.*;
import com.itq.entity.Document;
import com.itq.entity.History;
import com.itq.entity.enums.DocumentAction;
import com.itq.entity.enums.DocumentStatus;
import com.itq.entity.enums.StatusChangeResultType;
import com.itq.exception.DocumentNotFoundException;
import com.itq.exception.RegistryException;
import com.itq.exception.ValidationException;
import com.itq.repository.ApprovalRegistryRepository;
import com.itq.repository.DocumentRepository;
import com.itq.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final int MAX_BATCH_SIZE = 1000;

    private final DocumentRepository documentRepository;
    private final HistoryRepository historyRepository;
    private final ApproveOneExecutor approveOneExecutor;

    @Transactional
    public Long createDocument(DocumentCreateRequest req) {
        validateCreate(req);

        Document doc = new Document();
        doc.setAuthor(req.author().trim());
        doc.setTitle(req.title().trim());
        doc.setStatus(DocumentStatus.DRAFT);
        return documentRepository.save(doc).getId();
    }

    public DocumentResponse getDocument(Long id) {
        Document doc = documentRepository.findById(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));
        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public DocumentWithHistoryResponse getDocumentWithHistory(Long id) {
        Document doc = documentRepository.findById(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));
        List<History> historyList = historyRepository.findByDocumentIdOrderByCreatedAtAsc(id);
        return new DocumentWithHistoryResponse(
            toResponse(doc),
            historyList.stream().map(this::toHistoryResponse).toList()
        );
    }

    public List<DocumentResponse> getDocumentsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Document> docs = documentRepository.findByIdIn(ids);
        return docs.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BatchResultDto submit(BatchOpRequest req) {
        validateBatch(req);

        List<ItemResult> results = new ArrayList<>();
        for (Long id : req.ids()) {
            try {
                submitOne(id, req.initiator(), req.comment() != null ? req.comment() : "");
                results.add(new ItemResult(id, StatusChangeResultType.SUCCESS, "Submitted"));
            } catch (DocumentNotFoundException e) {
                results.add(new ItemResult(id, StatusChangeResultType.NOT_FOUND, "Document not found"));
            } catch (ValidationException e) {
                results.add(new ItemResult(id, StatusChangeResultType.CONFLICT, e.getMessage()));
            }
        }
        return new BatchResultDto(results);
    }

    @Transactional
    public BatchResultDto approve(BatchOpRequest req) {
        validateBatch(req);

        List<ItemResult> results = new ArrayList<>();
        for (Long id : req.ids()) {
            try {
                approveOneExecutor.execute(id, req.initiator(), req.comment() != null ? req.comment() : "");
                results.add(new ItemResult(id, StatusChangeResultType.SUCCESS, "Approved"));
            } catch (DocumentNotFoundException e) {
                results.add(new ItemResult(id, StatusChangeResultType.NOT_FOUND, "Document not found"));
            } catch (ValidationException e) {
                results.add(new ItemResult(id, StatusChangeResultType.CONFLICT, e.getMessage()));
            } catch (RegistryException e) {
                results.add(new ItemResult(id, StatusChangeResultType.REGISTRY_ERROR, e.getMessage()));
            }
        }
        return new BatchResultDto(results);
    }

    public Page<DocumentResponse> search(SearchCriteria criteria, Pageable pageable) {
        DocumentStatus status = criteria.status() != null && !criteria.status().isBlank()
            ? DocumentStatus.valueOf(criteria.status().trim().toUpperCase())
            : null;
        String author = criteria.author() != null && !criteria.author().isBlank()
            ? criteria.author().trim()
            : null;
        LocalDateTime from = criteria.fromDate();
        LocalDateTime to = criteria.toDate();

        Page<Document> page = documentRepository.search(status, author, from, to, pageable);
        return page.map(this::toResponse);
    }

    public Page<DocumentResponse> getDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable).map(this::toResponse);
    }

    public List<Document> findDraftBatch(int batchSize) {
        return documentRepository.findByStatusOrderByIdAsc(
            DocumentStatus.DRAFT,
            PageRequest.of(0, batchSize)
        );
    }

    public List<Document> findSubmittedBatch(int batchSize) {
        return documentRepository.findByStatusOrderByIdAsc(
            DocumentStatus.SUBMITTED,
            PageRequest.of(0, batchSize)
        );
    }

    @Transactional
    public void submitOne(Long id, String initiator, String comment) {
        Document doc = documentRepository.findById(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));

        if (doc.getStatus() != DocumentStatus.DRAFT) {
            throw new ValidationException("Document must be in DRAFT status to submit. Current: " + doc.getStatus());
        }

        doc.setStatus(DocumentStatus.SUBMITTED);
        documentRepository.save(doc);

        History history = new History();
        history.setDocument(doc);
        history.setAction(DocumentAction.SUBMIT);
        history.setInitiator(initiator);
        history.setComment(comment);
        historyRepository.save(history);
    }

    public void approveOne(Long id, String initiator, String comment) {
        approveOneExecutor.execute(id, initiator, comment);
    }

    private void validateCreate(DocumentCreateRequest req) {
        if (req == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (req.initiator() == null || req.initiator().isBlank()) {
            throw new ValidationException("Initiator cannot be empty");
        }
        if (req.author() == null || req.author().isBlank()) {
            throw new ValidationException("Author cannot be empty");
        }
        if (req.title() == null || req.title().isBlank()) {
            throw new ValidationException("Title cannot be empty");
        }
    }

    private void validateBatch(BatchOpRequest req) {
        if (req == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (req.initiator() == null || req.initiator().isBlank()) {
            throw new ValidationException("Initiator cannot be empty");
        }
        if (req.ids() == null || req.ids().isEmpty()) {
            throw new ValidationException("Ids cannot be empty");
        }
        if (req.ids().size() > MAX_BATCH_SIZE) {
            throw new ValidationException("Maximum " + MAX_BATCH_SIZE + " ids allowed");
        }
    }

    private DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(
            doc.getId(),
            doc.getUniqueNumber() != null ? doc.getUniqueNumber().toString() : null,
            doc.getAuthor(),
            doc.getTitle(),
            doc.getStatus(),
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }

    private HistoryResponse toHistoryResponse(History h) {
        return new HistoryResponse(
            h.getAction().name(),
            h.getInitiator(),
            h.getComment() != null ? h.getComment() : "",
            h.getCreatedAt()
        );
    }
}
