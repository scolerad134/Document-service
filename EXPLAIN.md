# EXPLAIN для поискового запроса

## Пример запроса

Поиск документов по фильтрам (статус, автор, период по дате создания). В API: `GET /api/documents/search?status=DRAFT&author=Generator&fromDate=2025-01-01T00:00:00&toDate=2025-12-31T23:59:59&page=0&size=20&sort=createdAt,desc`

Эквивалентный SQL:

```sql
SELECT d.id, d.unique_number, d.author, d.title, d.status, d.created_at, d.updated_at
FROM document d
WHERE d.status = 'DRAFT'
  AND d.author = 'Generator'
  AND d.created_at >= '2025-01-01 00:00:00'
  AND d.created_at <= '2025-12-31 23:59:59'
ORDER BY d.created_at DESC
LIMIT 20 OFFSET 0;
```

## EXPLAIN (ANALYZE)

```
Limit  (cost=8.58..8.59 rows=1 width=120) (actual time=0.045..0.046 rows=20 loops=1)
  ->  Sort  (cost=8.58..8.59 rows=1 width=120) (actual time=0.044..0.045 rows=20 loops=1)
        Sort Key: created_at DESC
        Sort Method: top-N heapsort  Memory: 28kB
        ->  Index Scan using idx_document_status on document d  (cost=0.15..8.57 rows=1 width=120) (actual time=0.023..0.035 rows=20 loops=1)
              Index Cond: (status = 'DRAFT'::character varying)
              Filter: ((author = 'Generator'::text) AND (created_at >= '2025-01-01 00:00:00'::timestamp) AND (created_at <= '2025-12-31 23:59:59'::timestamp))
              Rows Removed by Filter: 0
Planning Time: 0.123 ms
Execution Time: 0.075 ms
```

## Индексы

В Liquibase (001-create-tables.sql) созданы индексы:

| Индекс | Колонка | Назначение |
|--------|---------|------------|
| `idx_document_status` | status | Фильтр по статусу (DRAFT/SUBMITTED/APPROVED) — сужает выборку |
| `idx_document_author` | author | Фильтр по автору |
| `idx_document_created_at` | created_at | Период дат и сортировка ORDER BY created_at |

 planner использует `idx_document_status`, т.к. статус обычно даёт меньше строк, чем автор. Дополнительные условия (author, created_at) применяются как Filter. Для больших объёмов данных можно добавить составной индекс:

```sql
CREATE INDEX idx_document_search ON document (status, author, created_at DESC);
```

Он подойдёт для типичного поиска «статус + автор + период» и сортировки по дате.

## Период дат

В задании указано: «Как трактовать период (по дате создания или обновления) — выберите сами».

В реализации период (fromDate, toDate) применяется к полю **created_at** (дата создания).
