# Week 2 Blog API

This project implements a small blog backend with user registration, posts, and comments. It uses Java 21, Spring Boot, Spring MVC, Spring Data JPA, Flyway migrations, and a relational database. A file-backed H2 database is the default so the project can run locally without installing a database. An optional PostgreSQL profile and Compose file are included for a closer match to the Week 1 proposed stack.

## Requirements

- Java 21 or later
- Maven Wrapper (included; Maven will be downloaded by the wrapper if needed)
- Docker Compose only when using the optional PostgreSQL profile

## Run with the local H2 database

From this directory, run:

```powershell
./mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`. Flyway creates the tables on first start. H2 database files are stored in `data/` and ignored by Git.

## Run with PostgreSQL

Start the local database:

```powershell
docker compose up -d postgres
```

Then run the API using the PostgreSQL profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "local_blog_only_change_me"
./mvnw.cmd spring-boot:run
```

The sample password is for local development only. Set a private password in your local environment before exposing the database or app beyond your machine. The app runs migrations against the selected database at startup.

## Authentication

Registration and public reads do not require credentials. Creating, updating, and deleting posts or comments requires HTTP Basic authentication with the registered username and password. BCrypt is used to store password hashes. Only the user who created a post or comment can update or delete it. Use HTTPS and a production-grade authentication/session strategy before deploying this learning project publicly; HTTP Basic is intended here for a compact local exercise.

Register an account:

```powershell
curl.exe -X POST http://localhost:8080/api/v1/users `
  -H "Content-Type: application/json" `
  -d '{"username":"raj","email":"raj@example.com","password":"StrongPass123!"}'
```

Create a post:

```powershell
curl.exe -X POST http://localhost:8080/api/v1/posts `
  -u raj:StrongPass123! `
  -H "Content-Type: application/json" `
  -d '{"title":"My first post","content":"A short blog post."}'
```

## Endpoints

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| POST | `/api/v1/users` | Public | Register a user |
| GET | `/api/v1/posts` | Public | List posts, newest first |
| GET | `/api/v1/posts/{id}` | Public | Read one post |
| POST | `/api/v1/posts` | Authenticated | Create a post |
| PUT | `/api/v1/posts/{id}` | Owner | Replace a post's title and content |
| DELETE | `/api/v1/posts/{id}` | Owner | Delete a post and its comments |
| GET | `/api/v1/posts/{postId}/comments` | Public | List comments for a post |
| POST | `/api/v1/posts/{postId}/comments` | Authenticated | Add a comment |
| GET | `/api/v1/comments/{id}` | Public | Read one comment |
| PUT | `/api/v1/comments/{id}` | Owner | Edit a comment |
| DELETE | `/api/v1/comments/{id}` | Owner | Delete a comment |

Request bodies use JSON. Usernames are 3–50 letters, numbers, or underscores; passwords are 10–72 characters; post titles are at most 160 characters; post bodies are at most 10,000 characters; comments are at most 2,000 characters. Whitespace-only content is rejected. Usernames and email addresses are normalized to lowercase.

## Data model and error responses

Flyway migration `V1__create_blog_tables.sql` creates `blog_users`, `posts`, and `comments`, with unique user identifiers, foreign keys, indexes, and database checks. Deleting a post cascades to its comments. JPA validates the schema rather than generating tables.

The entity relationship diagram source is in `docs/data-model.mmd`.

Errors use a consistent JSON structure with a timestamp, HTTP status, error name, safe message, request path, and field-level validation errors when relevant. Expected statuses include `400` for invalid input, `401` for missing/invalid credentials, `403` for a non-owner attempting a change, `404` for a missing resource or endpoint, and `409` for duplicate username/email values. Unexpected errors return a generic message; details stay in the application log.

## Test and package

Run the integration suite against an in-memory H2 database:

```powershell
./mvnw.cmd test
```

The tests exercise registration and duplicate handling, request validation, public reads, authentication, ownership checks, post and comment CRUD, and not-found responses.

Build the executable JAR:

```powershell
./mvnw.cmd package
java -jar target/blog-api.jar
```

The generated JAR is in `target/`, which is excluded from source control.
