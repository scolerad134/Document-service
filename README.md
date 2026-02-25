# Document Service

Backend-сервис для работы с документами: создание, перевод по статусам (DRAFT → SUBMITTED → APPROVED), история изменений, реестр утверждений.

## Запуск

### 1. PostgreSQL (Docker Compose)

```bash
docker compose up -d
```

Создаётся БД `document-service`, пользователь/пароль: `postgres/postgres`, порт: 5432.

### 2. Сервис

```bash
mvn spring-boot:run -pl document-service
```

Сервис доступен на http://localhost:8080

**Для ручной проверки API без фоновой обработки** (документы не переводятся автоматически):

```bash
mvn spring-boot:run -pl document-service -Dspring-boot.run.profiles=manual
```

### 3. Swagger UI

http://localhost:8080/swagger-ui.html

---

## Утилита генерации документов

Создаёт N документов через API.

### Сборка

```bash
mvn -f document-generator/pom.xml package
```

### Конфигурация

Файл `document-generator/src/main/resources/generator.properties`:

```properties
count=100
api.baseUrl=http://localhost:8080/api
```

Либо передать при запуске: `-Dcount=500`

### Запуск

**Вариант 1** (из корня проекта, после `mvn -f document-generator/pom.xml package`):

```bash
java -cp "document-generator/target/classes:document-generator/target/lib/*" \
  -Dcount=100 \
  com.itq.generator.DocumentGeneratorMain
```

**Вариант 2** (Maven exec):

```bash
mvn -f document-generator/pom.xml exec:java -Dcount=100
```

**Вариант 3** (fat JAR, если настроен):

```bash
cd document-generator && mvn package
java -jar target/document-generator-0.0.1-SNAPSHOT.jar
```

Утилита выводит: сколько документов задано, прогресс (каждые 100 шт.), итог (success/failed) и общее время.

---

## Проверка прогресса по логам

### Генератор документов

```
Document Generator: creating 100 documents via http://localhost:8080/api
Progress: 100/100 created
Done. Total: 100, success=100, failed=0
Total time: 2500 ms
```

### Фоновые воркеры (SUBMIT / APPROVE)

В логах сервиса ищите строки:

```
SUBMIT-worker: batch of 50 documents processed in 120 ms, success=50, failed=0, осталось DRAFT=0
APPROVE-worker: batch of 50 documents processed in 95 ms, success=50, failed=0, осталось SUBMITTED=0
```

При ошибках:

```
SUBMIT-worker: failed items: [ItemResult[id=123, result=CONFLICT, message=...]]
```

### Создание документов через API

Логи Hibernate (если `spring.jpa.show-sql: true`) показывают INSERT. Для более структурированных логов можно добавить логирование в `DocumentController` или `DocumentService`.

---

## API (кратко)

| Метод | Путь | Описание |
|-------|------|----------|
| POST | /api/documents | Создать документ (DRAFT) |
| GET | /api/documents/{id}?withHistory=true | Документ с историей |
| GET | /api/documents?ids=1,2,3 | Пакетное получение |
| GET | /api/documents?page=0&size=20&sort=createdAt,desc | Пагинация |
| POST | /api/documents/submit | DRAFT → SUBMITTED |
| POST | /api/documents/approve | SUBMITTED → APPROVED |
| GET | /api/documents/search?status=DRAFT&author=... | Поиск с фильтрами |

---

## Тесты

```bash
mvn test -pl document-service
```

Покрыто:

| Сценарий | Класс | Метод |
|----------|-------|-------|
| happy-path: создание документа | DocumentServiceIntegrationTest | createDocument_happyPath |
| happy-path: submit одного документа | DocumentServiceIntegrationTest | submit_singleDocument |
| happy-path: approve одного документа | DocumentServiceIntegrationTest | approve_singleDocument |
| пакетный submit с частичными результатами | DocumentServiceIntegrationTest | submit_batch_partialResults |
| пакетный approve с частичными результатами | DocumentServiceIntegrationTest | approve_batch_partialResults |
| откат approve при ошибке записи в реестр | ApproveRegistryRollbackTest | approve_rollbackWhenRegistryFails |

---

## Опционально: масштабирование и вынос реестра

### Обработка запроса с 5000+ id

Сейчас лимит — 1000 id на запрос submit/approve. Чтобы уверенно обрабатывать 5000+ id:

1. **Обработка пачками внутри одного запроса** — разбить ids на подпачки (например по 200–500) и обрабатывать последовательно в рамках одного HTTP-запроса, собирая общий результат. Уменьшает размер одной транзакции и нагрузку на БД.

2. **Асинхронная модель** — вместо синхронного ответа возвращать `jobId`, а результат отдавать по `GET /jobs/{jobId}`. Запрос создаёт задачу, фоновый воркер обрабатывает пачками, результат пишется в кеш/БД.

3. **Batch size и транзакции** — для каждого id уже используется отдельная транзакция (`REQUIRES_NEW` в approve), поэтому можно поднять лимит. Но 5000 вызовов в одном HTTP-запросе дают большую нагрузку: стоит ограничить, например, 500–1000 за раз, а для больших объёмов — асинхронная модель.

4. **Connection pool** — при 5000 параллельных операций важен размер пула HikariCP (`maximum-pool-size`). Настраивать под нагрузку и лимиты БД.

5. **Таймауты** — при большом числе id увеличить HTTP timeout на клиенте и при необходимости `spring.datasource.hikari.connection-timeout`.

### Вынос реестра утверждений в отдельную систему

**Вариант A: отдельная БД**

- Создать второй DataSource (например `approvalRegistryDataSource`) и отдельный JPA-репозиторий.
- В `ApprovalRegistryWriter` инжектить `EntityManager` или `JdbcTemplate` для второй БД.
- Обе операции (обновление документа и запись в реестр) выполняются в рамках распределённой транзакции (JTA/Atomikos) или через паттерн Saga с компенсирующими действиями при сбое.

**Вариант B: отдельный HTTP-сервис**

- Реестр — отдельный микросервис с REST API: `POST /approvals` (documentId, approvedBy, approvedAt).
- `ApprovalRegistryWriter` превращается в HTTP-клиент (RestTemplate/WebClient), вызывает внешний сервис.
- Важно: сначала обновить документ в основной БД, затем вызвать реестр. При ошибке HTTP — откатить документ (как сейчас при `RegistryException`).
- Нужны retry, circuit breaker (Resilience4j) и idempotency key, чтобы при повторе не создавать дубли.

**Вариант C: event-driven (Kafka/RabbitMQ)**

- После approve публиковать событие `DocumentApproved(documentId, approvedBy, approvedAt)`.
- Отдельный сервис/консьюмер подписан на топик и пишет в реестр.
- Документ обновляется синхронно, запись в реестр — асинхронно. Нужна идемпотентность консьюмера.

---

## Конфигурация

- `document.workers.enabled` — вкл/выкл воркеры (по умолчанию `true`)
- `document.workers.batch-size` — размер пачки (по умолчанию 50)
- `document.workers.submit-interval-ms` — интервал SUBMIT-воркера (мс)
- `document.workers.approve-interval-ms` — интервал APPROVE-воркера (мс)
