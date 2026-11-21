# agile Project

**A Scrum project management web application** for organizing product backlogs, planning sprints, tracking work on a Kanban board, and managing project collaboration with secure, role-based access control.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-Backend-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/OpenAPI-API%20Documentation-6BA539?logo=openapiinitiative&logoColor=white" alt="OpenAPI" />
</p>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [API Documentation](#api-documentation)
- [Useful URLs](#useful-urls)
- [Project Structure](#project-structure)
- [Security Notes](#security-notes)

## Overview

Agile Project provides a centralized workspace for managing Scrum projects, from maintaining the product backlog to tracking sprint execution.

The application separates business API responsibilities from authentication and authorization, using dedicated services and PostgreSQL databases for application and identity data.

## Features

- **Product Backlog Management** — Organize and maintain the work planned for a project.
- **Sprint Planning** — Manage sprints and their associated backlog items.
- **Kanban Board** — Visualize sprint work and track progress through a board-style interface.
- **Project Management** — Organize projects and their related resources.
- **Issue Management** — Create and manage issues throughout the development workflow.
- **Comments** — Support collaboration through issue-related discussions.
- **Role-Based Access Control** — Protect resources using a separate authentication and authorization server.
- **REST API** — Expose application functionality through a documented HTTP API.
- **OpenAPI Documentation** — Explore endpoints and authorize API requests using JWT bearer tokens.

## Architecture

The application consists of a resource server, a dedicated authentication server, and separate PostgreSQL databases.

```mermaid
flowchart TB
    User["User / API Client"]
    Docs["OpenAPI UI"]
    Auth["Keycloak :8080"]
    API["Agile Resource Server<br/>REST API :8081"]
    AppDB[("Application PostgreSQL")]
    AuthDB[("Authentication PostgreSQL")]

    User --> API
    User --> Auth
    Docs --> API
    Docs -. "Obtain JWT" .-> Auth
    API -. "Validate access token" .-> Auth
    API --> AppDB
    Auth --> AuthDB
```

### Components

| Component | Responsibility | Local endpoint |
|---|---|---|
| `agile-server` | Spring Boot resource server and REST API | `http://localhost:8081` |
| `auth-server` | Authentication and authorization infrastructure | `http://localhost:8080` |
| Application PostgreSQL | Stores application data | Internal Docker network |
| Authentication PostgreSQL | Stores authentication-related data | Internal Docker network |

The services and databases are orchestrated through Docker Compose.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend runtime |
| Spring Boot | Backend application framework |
| PostgreSQL | Relational database |
| Docker | Containerization |
| Docker Compose | Local multi-service orchestration |
| JWT / Bearer Authentication | API request authentication |
| OpenAPI | REST API documentation |

*The stack above reflects the technologies identified in the project configuration. Additional frameworks and libraries may be present in the individual modules.*

## Prerequisites

Before starting, ensure you have:

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/)

**Java 21 is only required if you want to run the Spring Boot module directly on your machine rather than inside Docker.**

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd agile-project
```

Replace `<repository-url>` with the URL of your Git repository.

### 2. Start the application

Run the following command from the repository root:

```bash
docker compose -f compose.yaml up --build
```

Docker Compose builds and starts the application services and their database containers.

To run the stack in the background:

```bash
docker compose -f compose.yaml up --build -d
```

### 3. Verify the services

Once the services have started, the main endpoints are:

| Service | URL |
|---|---|
| Resource server | [http://localhost:8081](http://localhost:8081) |
| Authentication server | [http://localhost:8080](http://localhost:8080) |
| OpenAPI UI (Swagger) | [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html) |
| OpenAPI specification | [http://localhost:8081/api-docs](http://localhost:8081/api-docs) |

The API may require authentication for protected endpoints.

### 4. Stop the application

```bash
docker compose -f compose.yaml down
```

To remove the containers and associated Compose-managed volumes, use:

```bash
docker compose -f compose.yaml down -v
```

**Warning:** The `-v` option removes associated volumes and can permanently delete locally stored database data.

## Authentication

Agile Project uses a separate authentication and authorization server to issue access tokens for protected API requests.

### Realm configuration

The authentication server imports its initial realm configuration from:

```text
infrastructure/auth-server/realm-export.json
```

The imported configuration defines the following realm and client:

| Setting | Value |
|---|---|
| Realm | `agile-project` |
| Client ID | `agile-project-server` |
| Token endpoint | `http://localhost:8080/realms/agile-project/protocol/openid-connect/token` |

### Sample accounts

The imported realm contains two sample accounts for local development:

| Account | Client roles |
|---|---|
| `admin@agile.local` | `sm`, `client_user` |
| `developer@agile.local` | `dev`, `client_user` |

Their passwords and the client secret are defined in `infrastructure/auth-server/realm-export.json`.
Do not reuse them outside a local environment.

### Getting an access token

```bash
curl -X POST http://localhost:8080/realms/agile-project/protocol/openid-connect/token \
  -d grant_type=password \
  -d client_id=agile-project-server \
  -d client_secret=<client secret> \
  -d username=admin@agile.local \
  -d password=<password>
```

Send the `access_token` from the response as `Authorization: Bearer <token>`.
In the OpenAPI UI use the **Authorize** button and paste the token.
Ready-made requests are in `client/http/client.http` and in the Bruno collection (see [API tests](#api-tests-bruno)).

## Sprint analytics API

All endpoints require a bearer token. Errors use the body `{"code", "message", "timestamp"}`.

| Endpoint | Purpose |
|---|---|
| `GET /api/analytics/sprints/{sprintId}/summary` | Sprint report (WF-1) |
| `GET /api/analytics/sprints/{sprintId}/velocity` | Velocity (WF-2) |
| `GET /api/analytics/sprints/{sprintId}/burndown` | Burndown chart data (WF-3) |
| `GET /api/analytics/sprints/{sprintId}/burnup` | Burnup chart data (WF-4) |
| `GET /api/analytics/projects/{projectId}/sprints/comparison?from=&to=` | Sprint comparison with date filter (WF-5, WF-6) |
| `GET /api/analytics/projects/{projectId}/sprints/comparison.csv?from=&to=` | CSV export of the comparison (WF-7) |

The backend returns data only; drawing the charts is up to the client.
Calculation rules, edge cases and examples are described in `docs/analytics/`:

- `db-schema.md` - schema additions and ownership
- `wf1-wf2-metrics.md` - summary and velocity
- `wf3-wf4-charts.md` - burndown and burnup
- `wf5-wf7-reports-csv.md` - comparison, filtering, CSV and demo data

## Running locally without Docker for the application

The databases and Keycloak can run in Docker while the application runs on your machine (Java 21 required):

```bash
docker compose -f compose.yaml up -d app-db auth-db auth-server
mvn spring-boot:run
```

### Demo data

The `demo` profile loads two demo projects (Demo Alpha and Demo Beta) with finished and active sprints.
It uses a separate database, `agiledb_demo`. Create it once, then start the application with the profile:

```bash
docker compose -f compose.yaml exec app-db psql -U appuser -d agiledb -c "CREATE DATABASE agiledb_demo;"
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Do not use the `demo` profile against a production database.

## Tests and coverage

```bash
mvn clean verify
```

The build runs all tests and the JaCoCo coverage check.
The check requires at least 75% instruction coverage for the `analytics.reports` and `analytics.charts` packages.
The HTML report is written to `target/site/jacoco/index.html`.

## API tests (Bruno)

The folder `client/bruno/agile-project` contains a [Bruno](https://www.usebruno.com/) collection with requests and
assertions for the analytics endpoints. See `client/bruno/agile-project/README.md` for the setup.
