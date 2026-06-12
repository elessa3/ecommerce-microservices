# Ecommerce Microservices Platform

A production-ready e-commerce backend built with a **microservices architecture** using Java 21 and Spring Boot 3.3. Originally a monolithic REST API, this project was redesigned to demonstrate real-world distributed systems patterns used in the European tech industry.

## Architecture Overview

```
                          ┌─────────────────────────────────────┐
                          │           Client (React / Mobile)    │
                          └──────────────────┬──────────────────┘
                                             │ HTTPS
                          ┌──────────────────▼──────────────────┐
                          │          API Gateway (:8080)         │
                          │   Spring Cloud Gateway + JWT Auth    │
                          │         + Redis Rate Limiting        │
                          └──────┬────────────────┬─────────────┘
                                 │                │
               ┌─────────────────▼──┐    ┌────────▼─────────────┐
               │  Product Service   │    │   Order Service       │
               │      (:8081)       │    │      (:8082)          │
               │  Elasticsearch     │    │  Stripe Payments      │
               │  Redis Cache       │    │  State Machine        │
               └──────┬─────────────┘    └────────┬─────────────┘
                      │                           │
                      │         ┌─────────────────▼──────────────┐
                      │         │         Apache Kafka            │
                      │         │    Topics: order-events         │
                      │         └─────────────────┬──────────────┘
                      │                           │
                      │         ┌─────────────────▼──────────────┐
                      │         │    Notification Service (:8083) │
                      │         │    Kafka Consumer               │
                      │         │    Email (Spring Mail)          │
                      │         └────────────────────────────────┘
                      │
          ┌───────────▼──────────────────────────────────────────┐
          │                  Infrastructure                       │
          │  PostgreSQL  │  Redis  │  Kafka  │  Elasticsearch     │
          └──────────────────────────────────────────────────────┘
```

## Services

| Service | Port | Responsibility | Key Technologies |
|---|---|---|---|
| `gateway` | 8080 | Routing, JWT auth, rate limiting | Spring Cloud Gateway, Redis |
| `product-service` | 8081 | Product catalogue, search, stock | Elasticsearch, Redis Cache |
| `order-service` | 8082 | Orders, payments, state machine | Stripe, Kafka Producer |
| `notification-service` | 8083 | Emails and async notifications | Kafka Consumer, Thymeleaf |

## Tech Stack

**Backend**
- Java 21 · Spring Boot 3.3 · Spring Security 6
- Spring Cloud Gateway · Spring Cloud LoadBalancer
- Spring Data JPA / Hibernate · Spring Data Elasticsearch
- Apache Kafka (event-driven messaging)
- Redis (cache + rate limiting)
- PostgreSQL (primary database)

**Testing**
- JUnit 5 · Mockito
- Testcontainers (integration tests with real dependencies)

**Infrastructure & DevOps**
- Docker · Docker Compose (local development)
- Kubernetes (deployment manifests in `infra/k8s/`)
- GitHub Actions (CI/CD pipeline)
- Spring Boot Actuator (health checks + liveness/readiness probes)

## Key Architecture Decisions

**Why microservices?**
Each service has a distinct bounded context (catalogue vs orders vs notifications) with independent scaling requirements. The product catalogue is read-heavy and benefits from Elasticsearch + Redis. The order service is write-heavy with strong consistency needs.

**Why Kafka instead of synchronous REST between services?**
Decouples the order lifecycle from notification delivery. If the notification service is down, orders still process correctly — events are replayed when it comes back up. This avoids cascading failures.

**Why Redis in two places?**
- In the gateway: rate limiting per client IP (sliding window algorithm)
- In the product service: response caching to reduce Elasticsearch load for repeated queries

**Why a gateway for JWT validation?**
Centralising authentication means each downstream service trusts the `X-User-Id` header injected by the gateway. No duplicated security logic across services.

## Getting Started

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Run locally

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/ecommerce-microservices.git
cd ecommerce-microservices

# Start all infrastructure (PostgreSQL, Redis, Kafka, Elasticsearch)
docker compose -f infra/docker-compose.yml up -d

# Start each service (in separate terminals, or use your IDE)
cd product-service && mvn spring-boot:run
cd order-service   && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd gateway         && mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

### Run tests

```bash
# Unit + integration tests for all services (requires Docker for Testcontainers)
mvn test --projects product-service,order-service,notification-service,gateway
```

## API Reference

Full documentation is available via Swagger UI when each service is running:

| Service | Swagger URL |
|---|---|
| Product Service | http://localhost:8081/swagger-ui.html |
| Order Service | http://localhost:8082/swagger-ui.html |
| Gateway (aggregated) | http://localhost:8080/swagger-ui.html |

### Key endpoints (via gateway)

```
GET  /api/products?q=shoes&category=sport    # Full-text search (Elasticsearch)
GET  /api/products/{id}                      # Product detail (Redis cached)
POST /api/products                           # Create product (admin)

POST /api/orders                             # Place order → triggers Kafka event
GET  /api/orders/{id}                        # Order detail + status
POST /api/orders/{id}/cancel                 # Cancel order → triggers Kafka event
```

## Project Structure

```
ecommerce-microservices/
├── product-service/
├── order-service/
├── notification-service/
├── gateway/
├── infra/
│   ├── docker-compose.yml
│   └── k8s/
├── .github/
│   └── workflows/
│       └── ci.yml
├── pom.xml                  ← parent POM (shared dependency versions)
└── README.md
```

## CI/CD

GitHub Actions runs on every push to `main` and every pull request:
1. Compile all services
2. Run unit and integration tests (Testcontainers spins up real Docker containers)
3. Build Docker images
4. Push to Docker Hub

See `.github/workflows/ci.yml` for the full pipeline definition.

## What I'd Add Next

- **Distributed tracing** with Micrometer + Zipkin (trace a request across all services)
- **Service discovery** with Spring Cloud Eureka (dynamic service registration)
- **Circuit breaker** with Resilience4j (graceful degradation when a service is slow)
- **GDPR compliance**: right to erasure, data export, cookie consent
- **European payment methods**: iDEAL, Bancontact, SEPA via Mollie

---

*This project evolved from a monolithic Spring Boot API built as a university project. The redesign to microservices was driven by the architecture patterns most commonly required in the European backend Java market.*
