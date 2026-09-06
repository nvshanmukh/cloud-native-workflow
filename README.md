# Cloud-Native Workflow & Job Orchestration Platform

A highly scalable, distributed workflow orchestration engine inspired by concepts from AWS Step Functions, Temporal, and Celery. Built with Java 21 and Spring Boot.

## 🚀 Key Features
- **DAG Dependency Resolution:** Dynamically maps and resolves complex Directed Acyclic Graph (DAG) task dependencies using a custom RDBMS join-table implementation.
- **Event-Driven Architecture:** Decouples API ingestion from execution using **Apache Kafka** for massive asynchronous throughput.
- **Strict At-Least-Once Delivery:** Uses Kafka manual acknowledgments (`MANUAL_IMMEDIATE`) paired with database idempotency locks to guarantee zero data loss during worker crashes.
- **Reliability Engineering:** Implements Scheduled Exponential Backoff Retries and Dead-Letter Queues (DLQ) for fault tolerance.
- **High Performance:** Tuned with HikariCP connection pooling, Snappy Kafka payload compression, and `jsonb` PostgreSQL columns.
- **Observability:** Built-in Prometheus metrics and structured JSON logging (Logstash/ELK) for production telemetry.
- **Rate Limiting:** Distributed Redis-backed sliding window rate limiter to prevent API abuse.

## 🏗️ Architecture

### High-Level System Design
1. **API Layer:** Stateless REST API secured by JWTs.
2. **Orchestration Layer:** The "Brain". Evaluates DAGs, transitions states, and publishes `READY` tasks to Kafka.
3. **Messaging Layer (Kafka):** The "Spine". Provides backpressure, durability, and consumer load-balancing.
4. **Worker Layer:** The "Muscle". Horizontally scalable workers that consume tasks, execute strategies, and notify the Orchestrator upon completion.
5. **Persistence (PostgreSQL):** Source of truth for Workflow states and relationships.
6. **Cache (Redis):** Fast reads for workflow statuses and distributed rate limiting.

### AWS Production Mapping
If deployed to AWS, this local architecture maps directly to managed services:
- **Spring Boot API & Workers** -> AWS ECS (Fargate) or EKS
- **PostgreSQL** -> Amazon RDS for PostgreSQL (Multi-AZ)
- **Kafka** -> Amazon MSK (Managed Streaming for Kafka)
- **Redis** -> Amazon ElastiCache for Redis
- **Load Balancer** -> AWS ALB
- **Observability** -> Amazon CloudWatch (Logs) & Amazon Managed Grafana (Prometheus)

## 🛠️ Tech Stack
- **Backend:** Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, Spring Kafka
- **Infrastructure:** PostgreSQL 15, Apache Kafka (KRaft), Redis 7
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Build/Deploy:** Maven, Docker, GitHub Actions

## 🚦 Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21

### Run Locally
1. Start the infrastructure (Postgres, Kafka, Redis):
```bash
docker compose up -d
```
2. Run the application:
```bash
./mvnw spring-boot:run
```
3. Access Kafka UI at `http://localhost:8081`

### Test
Run the full suite (including Testcontainers integration tests):
```bash
./mvnw clean verify
```

## 📈 Scalability & Tradeoffs
- **Postgres JSONB:** Traded strict schema validation for arbitrary task payload flexibility.
- **Orchestration vs Choreography:** Chose Orchestration (Centralized Brain) over Choreography (Decentralized Events) to maintain a single source of truth for complex DAG states, making observability much simpler at the cost of database contention.
- **Polling vs Push:** Avoided DB polling. The Orchestrator actively "pushes" state changes forward to child tasks, reducing DB CPU load by relying on event-driven wakeups.
