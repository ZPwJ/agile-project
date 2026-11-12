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

### 2. Configure `.env`

Compose reads the passwords from a `.env` file in the repository root. Create it from the example and fill in the values:

```bash
cp .env.example .env
```

- `POSTGRES_PASSWORD` and `SPRING_DATASOURCE_PASSWORD` have to be the same (app database),
- `AUTH_DB_POSTGRES_PASSWORD` and `KC_DB_PASSWORD` have to be the same (Keycloak database),
- `AUTH_SERVER_CLIENT_SECRET` is the `secret` of the `agile-project-server` client in `infrastructure/auth-server/realm-export.json`.

The other passwords can be anything. Postgres sets the password only when its volume is created, so if you change it later you have to recreate the volume with `docker compose down -v` (this deletes the data).

### 3. Start the application

Run the following command from the repository root:

```bash
docker compose -f compose.yaml up --build
```

Docker Compose builds and starts the application services and their database containers.

To run the stack in the background:

```bash
docker compose -f compose.yaml up --build -d
```

### 4. Verify the services

Once the services have started, the main endpoints are:

| Service | URL |
|---|---|
| Resource server | [http://localhost:8081](http://localhost:8081) |
| Authentication server | [http://localhost:8080](http://localhost:8080) |
| OpenAPI UI | [http://localhost:8081/swagger-ui](http://localhost:8081/swagger-ui) |
| OpenAPI specification | [http://localhost:8081/api-docs](http://localhost:8081/api-docs) |

Swagger UI is public, the API itself needs a Bearer token (see [Getting a token](#getting-a-token)).

### 5. Stop the application

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

The realm comes with two test users:

| Username | Password | Role |
|---|---|---|
| `admin@agile.local` | `admin123` | `sm` |
| `developer@agile.local` | `dev123` | `dev` |

### Getting a token

```bash
source .env
curl -X POST http://localhost:8080/realms/agile-project/protocol/openid-connect/token \
  -d grant_type=password \
  -d client_id=agile-project-server \
  -d client_secret=$AUTH_SERVER_CLIENT_SECRET \
  -d username=admin@agile.local \
  -d password=admin123
```

Take `access_token` from the response and send it as a Bearer token. In Swagger use the **Authorize** button, with curl:

```bash
curl http://localhost:8081/api/scrum-project -H "Authorization: Bearer <access_token>"
```

When the token expires, just request a new one.

## Tests

Requires Java 21 and Maven:

```bash
mvn verify
```

Tests run on an in-memory H2 database, so Docker and `.env` are not needed. The JaCoCo report is generated in `target/site/jacoco/index.html`.

## Dictionaries

When running on PostgreSQL, Spring executes `src/main/resources/data-postgres.sql` on startup. It inserts the issue statuses, types and priorities. Rows that already exist are skipped (`ON CONFLICT DO NOTHING`), so restarting the app doesn't duplicate or overwrite anything.
