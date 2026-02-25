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
mvn spring-boot:run
```

Сервис доступен на http://localhost:8080

**Для ручной проверки API без фоновой обработки** (документы не переводятся автоматически):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=manual
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
SUBMIT-worker: batch of 50 documents processed in 120 ms, success=50, failed=0
APPROVE-worker: batch of 50 documents processed in 95 ms, success=50, failed=0
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
mvn test
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

## Конфигурация

- `document.workers.enabled` — вкл/выкл воркеры (по умолчанию `true`)
- `document.workers.batch-size` — размер пачки (по умолчанию 50)
- `document.workers.submit-interval-ms` — интервал SUBMIT-воркера (мс)
- `document.workers.approve-interval-ms` — интервал APPROVE-воркера (мс)
