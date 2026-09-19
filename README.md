# Service Order Management API

[![CI](https://github.com/otaldoneto/Project-Service-Order-Management/actions/workflows/ci.yml/badge.svg)](https://github.com/otaldoneto/Project-Service-Order-Management/actions/workflows/ci.yml)

REST API for managing clients, technicians and service orders (help desk style), built with Spring Boot.

## Features

- Endpoints for **clients** and **technicians** with bean validation
- **Service order** lifecycle with enforced status transitions
- Mandatory **root cause report** to finish an order
- **PDF report** generation for any service order
- Consistent JSON error responses
- Interactive API docs with Swagger UI

## Tech stack

- Java 25
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation)
- H2 in-memory database and PostgreSQL
- springdoc-openapi 3.0.3 (Swagger UI)
- OpenPDF 2.0.3
- Lombok
- JUnit 5 and Mockito

## Getting started

Requirements: JDK 25.

```bash
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080` and the Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

By default the application uses an in-memory H2 database, so data is reset every time it restarts.

Run the tests:

```bash
./mvnw test
```

## Configuration profiles

| Profile | Database     | Notes                                                         |
|---------|--------------|---------------------------------------------------------------|
| default | H2 in-memory | Swagger UI enabled, data is reset on restart                  |
| `prod`  | PostgreSQL   | Swagger UI disabled, configured through environment variables |

Run with PostgreSQL:

```bash
DB_URL=jdbc:postgresql://localhost:5432/service_orders \
DB_USERNAME=postgres \
DB_PASSWORD=postgres \
SPRING_PROFILES_ACTIVE=prod \
./mvnw spring-boot:run
```

## Endpoints

| Method | Path                                 | Description                                        |
|--------|--------------------------------------|----------------------------------------------------|
| GET    | `/clients`                           | List clients                                       |
| GET    | `/clients/{id}`                      | Get a client                                       |
| POST   | `/clients`                           | Create a client                                    |
| GET    | `/technicians`                       | List technicians                                   |
| GET    | `/technicians/{id}`                  | Get a technician                                   |
| POST   | `/technicians`                       | Create a technician                                |
| GET | `/orders` | List service orders (paginated: `page`, `size`, `sort`) || GET    | `/orders/{id}`                       | Get a service order                                |
| POST   | `/orders`                            | Create a service order for a client                |
| PUT    | `/orders/{id}/assign/{technicianId}` | Assign a technician (status becomes `IN_PROGRESS`) |
| PUT    | `/orders/{id}/finish`                | Finish an order with a root cause report           |
| PUT    | `/orders/{id}/cancel`                | Cancel an order                                    |
| GET    | `/orders/{id}/report`                | Download the order report as PDF                   |

## Pagination

`GET /orders` is paginated. Query parameters: `page` (starts at 0), `size` (default 20, max 100) and `sort` (default `createdAt,desc`).

```json
{
  "content": [ { "id": 3, "title": "Order 3", "...": "..." } ],
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

| Status | When                                               |
|--------|----------------------------------------------------|
| 400    | Invalid input, malformed JSON or invalid parameter |
| 404    | Resource not found                                 |
| 409 | Status transition not allowed, or duplicated email / CPF/CNPJ |

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.