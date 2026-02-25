package com.itq.controller;

import com.itq.dto.*;
import com.itq.service.ConcurrentApprovalService;
import com.itq.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Документы", description = "CRUD и операции со статусами документов")
public class DocumentController {
    private final DocumentService documentService;
    private final ConcurrentApprovalService concurrentApprovalService;

    @Operation(summary = "Создать документ", description = "Создаёт документ в статусе DRAFT. Уникальный номер генерируется автоматически.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Документ создан"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации (пустой автор/название)")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createDocument(@Valid @RequestBody DocumentCreateRequest request) {
        Long id = documentService.createDocument(request);
        return Map.of("id", id);
    }

    @Operation(summary = "Получить документ", description = "Возвращает документ по id. С withHistory=true — вместе с историей изменений статусов.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Документ найден"),
        @ApiResponse(responseCode = "404", description = "Документ не найден")
    })
    @GetMapping("/{id}")
    public Object getDocument(
        @Parameter(description = "ID документа") @PathVariable Long id,
        @Parameter(description = "Включить историю изменений") @RequestParam(defaultValue = "false") boolean withHistory
    ) {
        if (withHistory) {
            return documentService.getDocumentWithHistory(id);
        }
        return documentService.getDocument(id);
    }

    @Operation(summary = "Список документов", description = "Пачка по ids — возвращает документы по списку id. Без ids — постраничный список с пагинацией и сортировкой.")
    @GetMapping
    public Object getDocuments(
        @Parameter(description = "Список id для пакетного получения") @RequestParam(required = false) List<Long> ids,
        @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "Поле сортировки") @RequestParam(defaultValue = "createdAt") String sortBy,
        @Parameter(description = "Направление: asc/desc") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (ids != null && !ids.isEmpty()) {
            return documentService.getDocumentsByIds(ids);
        }
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return documentService.getDocuments(pageable);
    }

    @Operation(summary = "Отправить на согласование", description = "Переводит документы DRAFT → SUBMITTED. Принимает 1–1000 id. Для каждого id: успех / конфликт / не найден.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Результат по каждому id")
    })
    @PostMapping("/submit")
    public BatchResultDto submit(@Valid @RequestBody BatchOpRequest request) {
        return documentService.submit(request);
    }

    @Operation(summary = "Утвердить", description = "Переводит документы SUBMITTED → APPROVED. Создаёт запись в реестре утверждений. При ошибке записи — откат. 1–1000 id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Результат: успех / конфликт / не найден / ошибка регистрации")
    })
    @PostMapping("/approve")
    public BatchResultDto approve(@Valid @RequestBody BatchOpRequest request) {
        return documentService.approve(request);
    }

    @Operation(summary = "Поиск документов", description = "Фильтры: статус, автор, период по дате создания (fromDate, toDate). ISO 8601 для дат.")
    @GetMapping("/search")
    public Page<DocumentResponse> search(
        @Parameter(description = "DRAFT | SUBMITTED | APPROVED") @RequestParam(required = false) String status,
        @Parameter(description = "Автор документа") @RequestParam(required = false) String author,
        @Parameter(description = "Начало периода (createdAt)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
        @Parameter(description = "Конец периода (createdAt)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        SearchCriteria criteria = new SearchCriteria(status, author, fromDate, toDate);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return documentService.search(criteria, pageable);
    }

    @Operation(summary = "Тест конкурентного утверждения", description = "Запускает threads×attempts параллельных попыток утвердить один документ. Ожидается: ровно 1 успех, остальные — конфликт.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Сводка: success/conflict/error count, финальный статус")
    })
    @PostMapping("/concurrent-approve-test")
    public ConcurrentApprovalResult concurrentApprovalTest(@Valid @RequestBody ConcurrentApprovalRequest request) {
        return concurrentApprovalService.runConcurrentApproval(
                request.documentId(),
                request.threads(),
                request.attempts(),
                request.initiator()
        );
    }
}
