# Notes App

Простое приложение для заметок на `Ktor` и `React`.

## Запуск

```bash
./gradlew run
```

Для локального запуска нужен PostgreSQL и переменные окружения:

```bash
export JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/notes
export JDBC_DATABASE_USERNAME=notes
export JDBC_DATABASE_PASSWORD=replace-with-a-strong-password
```

После запуска приложение будет доступно на `http://localhost:8080`.

## Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

После этого поднимутся и приложение, и PostgreSQL. Заметки будут храниться в Docker volume.
Если вы меняли `POSTGRES_PASSWORD`, а volume уже был создан раньше, проще пересоздать базу:

```bash
docker compose down -v
docker compose up --build
```

## Тесты

```bash
./gradlew test
```

## API

### Получить все заметки

```http
GET /api/notes
```

### Получить заметку по id

```http
GET /api/notes/{id}
```

### Создать заметку

```http
POST /api/notes
Content-Type: application/json

{
  "title": "План на вечер",
  "content": "Сделать демо и проверить UI"
}
```

### Обновить заметку

```http
PUT /api/notes/{id}
Content-Type: application/json

{
  "title": "Обновленный план",
  "content": "Поправить текст и сохранить"
}
```

### Удалить заметку

```http
DELETE /api/notes/{id}
```
