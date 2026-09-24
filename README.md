# Service Order Management API

[![CI](https://github.com/otaldoneto/Project-Help-Desk/actions/workflows/ci.yml/badge.svg)](https://github.com/otaldoneto/Project-Help-Desk/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/otaldoneto/Project-Help-Desk/graph/badge.svg)](https://codecov.io/gh/otaldoneto/Project-Help-Desk)

REST API for managing clients, technicians and service orders (help desk style), built with Spring Boot.

## Live demo

- Home page: https://service-order-management-nxil.onrender.com
- Swagger UI: https://service-order-management-nxil.onrender.com/swagger-ui.html

The demo runs on free tiers ([Render](https://render.com) for the API and [Neon](https://neon.com) for PostgreSQL), so
the API goes to sleep after 15 minutes without traffic and the first request afterwards can take about a minute. The
administrator credentials are private; to try the API yourself, run it locally (see below), where an administrator is
created for you.

## Features

- Endpoints for **clients** and **technicians** with bean validation
- **Service order** lifecycle with enforced status transitions
- Mandatory **root cause report** to finish an order
- **PDF report** generation for any service order
- Consistent JSON error responses
- Interactive API docs with Swagger UI
- **CPF/CNPJ validation** (check digits), including the new alphanumeric CNPJ
- **Optimistic locking** on service orders: two conflicting concurrent updates never overwrite each other silently (the
  loser gets `409`)
- **JWT authentication** with three roles (`ADMIN`, `USER` and the read-only `VIEWER`) and BCrypt-hashed passwords
- **Audit trail** on service orders: every order records who created it and who last changed it (`createdBy`,
  `lastModifiedBy`, `lastModifiedAt`), taken from the authenticated user's email

## Tech stack

- Java 25
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation)
- H2 in-memory database and PostgreSQL
- springdoc-openapi 3.0.3 (Swagger UI)
- OpenPDF 2.0.3
- Lombok
- JUnit 5 and Mockito
- Flyway (database migrations)

## Getting started

Requirements: JDK 25.

Run the application (default profile: in-memory H2 database, Swagger UI enabled):

```bash
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080` and the Swagger UI is available at `http://localhost:8080/swagger-ui.html`. Data
is reset every time the application restarts.

Run the tests:

```bash
./mvnw test
```

The test run also produces a coverage report (JaCoCo) in `target/site/jacoco/index.html`. The build fails if line
coverage drops below 90%.

## Run with Docker

Everything (API and PostgreSQL) starts with one command:

```bash
cp .env.example .env
docker compose up --build
```

Edit `.env` to change the passwords and the JWT secret. The API runs on `http://localhost:8080`, and Swagger UI is
available when `SWAGGER_ENABLED=true`.

To start only the database (for example, to run the application from your IDE):

```bash
docker compose up -d --wait db
```

## Configuration profiles

| Profile | Database     | Notes                                                                                                                                  |
|---------|--------------|----------------------------------------------------------------------------------------------------------------------------------------|
| default | H2 in-memory | Swagger UI enabled, data is reset on restart                                                                                           |
| `prod`  | PostgreSQL   | Schema managed by Flyway migrations (`src/main/resources/db/migration`), Swagger UI disabled, configured through environment variables |

Run with PostgreSQL. A local database is available through `docker compose up -d --wait db`.

```bash
DB_URL=jdbc:postgresql://localhost:5432/service_orders \
DB_USERNAME=postgres \
DB_PASSWORD=postgres \
JWT_SECRET=<at-least-32-characters> \
ADMIN_EMAIL=admin@example.com \
ADMIN_PASSWORD=<choose-a-strong-password> \
SPRING_PROFILES_ACTIVE=prod \
./mvnw spring-boot:run
```

## Deploying to Render and Neon

The live demo is a Render **Web Service** built from the `Dockerfile` (Docker runtime, free plan) that connects to a
Neon PostgreSQL database. Set these environment variables on the service:

| Variable                           | Value                                                                        |
|------------------------------------|------------------------------------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`           | `prod`                                                                       |
| `DB_URL`                           | `jdbc:postgresql://<neon-host>/<database>?sslmode=require` (direct host, not the `-pooler` one) |
| `DB_USERNAME`, `DB_PASSWORD`       | The database credentials shown by Neon                                       |
| `JWT_SECRET`                       | A random secret of at least 32 characters                                    |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`    | The initial administrator                                                    |
| `SPRINGDOC_API_DOCS_ENABLED`, `SPRINGDOC_SWAGGER_UI_ENABLED` | `true` to expose Swagger UI (off by default in `prod`) |
| `SERVER_FORWARD_HEADERS_STRATEGY`  | `native`, so the application knows it is served over HTTPS behind a proxy    |
| `APP_CLIENT_IP_HEADER`             | `CF-Connecting-IP`, so the login rate limit sees the visitor's real IP (Render sits behind Cloudflare) |

Render provides the `PORT` variable by itself, and the application listens on it. Leave `APP_CLIENT_IP_HEADER` unset
anywhere the application is not behind a proxy that overwrites that header, otherwise clients could spoof their IP.

## Authentication and authorization

The API uses stateless **JWT** authentication. Log in to get a token and send it in every request:

```bash
curl -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"admin12345"}'
```

```bash
curl localhost:8080/clients -H "Authorization: Bearer <accessToken>"
```

In Swagger UI, log in through `POST /auth/login`, click **Authorize** and paste the token.

| Role     | Can do                                                                                                       |
|----------|--------------------------------------------------------------------------------------------------------------|
| `VIEWER` | Read everything and download PDF reports, nothing else. Meant for a shared, public demo account              |
| `USER`   | Read everything, create clients, technicians and service orders, move service orders through their lifecycle |
| `ADMIN`  | Everything a `USER` can do, plus update and delete clients and technicians, and create users (`POST /users`) |

A `VIEWER` gets `403` on every write, including changing its own password, so a shared demo account can be handed
out without anyone being able to change data or lock the others out. To create one, log in as `ADMIN` and call
`POST /users` with `"role": "VIEWER"`.

`/auth/login`, Swagger UI and the OpenAPI docs are public. Everything else needs a valid token.

**Development defaults:** in the default profile an administrator is created on startup (`admin@example.com` /
`admin12345`) and the JWT secret has a development value. These defaults exist for local use only. The `prod` profile
has **no defaults** and refuses to start without `JWT_SECRET`, `ADMIN_EMAIL` and `ADMIN_PASSWORD`.

Access tokens are signed with HS256 and expire after 15 minutes. Refresh tokens are opaque random strings, valid for
7 days, and can be exchanged for a new pair at `POST /auth/refresh` (each exchange rotates the refresh token: the
old one stops working). `POST /auth/logout` revokes a refresh token immediately. Passwords are stored as BCrypt
hashes.

After 5 failed login attempts from the same IP address within 15 minutes, further attempts (even with the correct
password) are rejected with `429 Too Many Requests` and a `Retry-After` header, until the window passes. A successful
login resets the counter.

## Endpoints

| Method | Path                                 | Description                                             |
|--------|--------------------------------------|---------------------------------------------------------|
| GET    | `/clients`                           | List clients (paginated)                                |
| GET    | `/clients/{id}`                      | Get a client                                            |
| POST   | `/clients`                           | Create a client                                         |
| GET    | `/technicians`                       | List technicians (paginated)                            |
| GET    | `/technicians/{id}`                  | Get a technician                                        |
| POST   | `/technicians`                       | Create a technician                                     |
| GET    | `/orders`                            | List service orders (paginated: `page`, `size`, `sort`) |
| POST   | `/orders`                            | Create a service order for a client                     |
| GET    | `/orders/{id}`                       | Get a service order                                     |
| PUT    | `/orders/{id}/assign/{technicianId}` | Assign a technician (status becomes `IN_PROGRESS`)      |
| PUT    | `/orders/{id}/finish`                | Finish an order with a root cause report                |
| PUT    | `/orders/{id}/cancel`                | Cancel an order                                         |
| GET    | `/orders/{id}/report`                | Download the order report as PDF                        |
| PUT    | `/clients/{id}`                      | Update a client                                         |
| DELETE | `/clients/{id}`                      | Delete a client (409 if it has service orders)          |
| PUT    | `/technicians/{id}`                  | Update a technician                                     |
| DELETE | `/technicians/{id}`                  | Delete a technician (409 if it has service orders)      |
| POST   | `/users`                             | Create a user (ADMIN only)                              |
| GET    | `/users`                             | List users (ADMIN only, paginated)                      |
| PUT    | `/users/{id}/disable`                | Disable a user (ADMIN only)                             |
| PUT    | `/users/{id}/enable`                 | Re-enable a user (ADMIN only)                           |
| PUT    | `/users/me/password`                 | Change your own password                                |

## Pagination

`GET /orders`, `GET /clients` and `GET /technicians` are paginated. Query parameters: `page` (starts at 0), `size`
(default 20, max 100) and `sort`. Default sort: `createdAt,desc` for orders and `name,asc` for clients and technicians.

```json
{
  "content": [
    {
      "id": 3,
      "title": "Order 3",
      "...": "..."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

## Service order lifecycle

```
            assign              finish
   OPEN ------------> IN_PROGRESS ------------> FINISHED
     |                     |
     | cancel              | cancel
     v                     v
              CANCELED
```

- A technician can be assigned while the order is `OPEN` or `IN_PROGRESS`.
- An order can only be finished when it is `IN_PROGRESS`, and a root cause report is required.
- An order can be canceled while it is `OPEN` or `IN_PROGRESS`.
- Any other transition is rejected with `409 Conflict`.

## Error responses

All errors share the same JSON format:

```json
{
  "timestamp": "2026-09-19T15:31:34.227968Z",
  "status": 404,
  "error": "Resource not found",
  "message": "Service Order not found with id: 999",
  "path": "/orders/999"
}
```

| Status | When                                                                                                                                             |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| 400    | Invalid input, malformed JSON or invalid parameter                                                                                               |
| 404    | Resource not found                                                                                                                               |
| 409    | Status transition not allowed, duplicated email / CPF/CNPJ, deleting a client / technician that has service orders, or a concurrent modification |
| 401    | Missing, invalid or expired token, or wrong email or password on login                                                                           |
| 403    | The authenticated user does not have the required role                                                                                           |
| 429    | Too many failed login attempts from this IP address                                                                                              |

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.