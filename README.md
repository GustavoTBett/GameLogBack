# Gamelog API

## PT-BR

### Visao geral

O Gamelog e uma API para organizar experiencias com jogos em uma unica plataforma.

Ela permite que usuarios:
- criem conta e facam login;
- registrem jogos favoritos;
- avaliem jogos com nota e comentario;
- consultem informacoes de jogos, generos e relacionamentos jogo-genero.

### Problema que o projeto resolve

Muitos jogadores usam varias plataformas separadas para descobrir, avaliar e acompanhar jogos.
O Gamelog centraliza esse fluxo para facilitar:
- historico pessoal de jogos e avaliacoes;
- organizacao por favoritos;
- futura recomendacao personalizada baseada no perfil do usuario.

### Status atual do projeto

Implementado hoje:
- autenticacao por sessao com Spring Security;
- gerenciamento de usuarios;
- favoritos;
- avaliacoes;
- consulta de jogos, generos e jogo-genero.

Roadmap (planejado):
- recomendacoes com IA com base no historico do usuario;
- descoberta de jogos menos populares e independentes;
- endpoints dedicados para sugestoes personalizadas.

Observacao: o projeto ja possui dependencia de Spring AI/OpenAI no build, mas os endpoints de recomendacao ainda nao estao expostos.

### Stack

- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Security (sessao + CSRF)
- Spring Data JPA
- Liquibase
- PostgreSQL
- Maven Wrapper

### Arquitetura

Padrao em camadas:
- controller: entrada HTTP
- service: regras de negocio
- repository: acesso a dados
- model: entidades JPA
- dto: contratos de request/response
- validation: validacoes customizadas
- exception: tratamento padronizado de erros

### Como rodar localmente

1. Suba o banco com Docker Compose:

```bash
docker-compose up -d
```

2. Rode a API:

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

API padrao: `http://localhost:8080`

### Variaveis e configuracao

Em `src/main/resources/application.properties`:
- banco: `jdbc:postgresql://localhost:5432/postgres`
- usuario/senha padrao: `postgres/postgres`
- cookie de sessao: `GAMELOG_SESSION`

Variaveis opcionais:
- `APP_SECURITY_ALLOWED_ORIGINS`
- `APP_SECURITY_COOKIE_SECURE`
- `OPENAI_API_KEY` (para evolucoes com IA)

### Autenticacao (importante)

Esta API usa sessao HTTP + CSRF (nao JWT).

Fluxo recomendado:
1. `GET /auth/csrf` para obter token CSRF
2. `POST /users` para criar conta
3. `POST /auth/login` com `identifier` (email ou username) e `password`
4. enviar cookie de sessao + header CSRF em requests de escrita

### Endpoints atuais

Autenticacao:
- `GET /auth/csrf` (publico)
- `POST /auth/login` (publico)
- `POST /auth/logout` (autenticado)
- `GET /auth/me` (autenticado)

Usuarios:
- `POST /users` (publico)
- `GET /users/{id}` (autenticado)
- `PUT /users/{id}` (autenticado)
- `DELETE /users/{id}` (admin)

Jogos:
- `GET /games/{id}` (autenticado)
- `DELETE /games/{id}` (autenticado)

Generos:
- `GET /genres/{id}` (autenticado)

Jogo-genero:
- `GET /game-genres/{id}` (autenticado)

Favoritos:
- `POST /favorites` (autenticado)
- `GET /favorites/{id}` (autenticado)
- `DELETE /favorites/{id}` (autenticado)

Avaliacoes:
- `POST /ratings` (autenticado)
- `GET /ratings/{id}` (autenticado)
- `PUT /ratings/{id}` (autenticado)
- `DELETE /ratings/{id}` (autenticado)

### Exemplos de payload

Criar usuario (`POST /users`):

```json
{
  "email": "user@example.com",
  "username": "user123",
  "password": "StrongPass123",
  "avatarUrl": "https://example.com/avatar.png",
  "bio": "Meu perfil gamer"
}
```

Login (`POST /auth/login`):

```json
{
  "identifier": "user@example.com",
  "password": "StrongPass123"
}
```

Criar favorito (`POST /favorites`):

```json
{
  "userId": 1,
  "gameId": 2
}
```

Criar avaliacao (`POST /ratings`):

```json
{
  "userId": 1,
  "gameId": 2,
  "score": 5,
  "review": "Excelente jogo"
}
```

### Testes e build

```bash
./mvnw test
./mvnw package
```

Windows (PowerShell):

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

### Documentacao para contribuicao tecnica

Consulte `CONTRIBUTING.md` para ver como criar nova entidade, controller e demais componentes no padrao do projeto.

---

## EN-US

### Overview

Gamelog is an API that helps users keep their gaming experience in one place.

It allows users to:
- create an account and log in;
- store favorite games;
- rate games with score and review;
- query games, genres, and game-genre associations.

### Problem being solved

Many players rely on fragmented tools to discover, rate, and track games.
Gamelog centralizes this process to improve:
- personal game and rating history;
- favorites organization;
- future personalized recommendation flows.

### Current project status

Implemented now:
- session-based authentication with Spring Security;
- user management;
- favorites;
- ratings;
- read operations for games, genres, and game-genre relations.

Roadmap (planned):
- AI recommendations based on user history;
- better discovery for indie and less popular titles;
- dedicated personalized recommendation endpoints.

Note: the build already includes Spring AI/OpenAI dependencies, but recommendation endpoints are not exposed yet.

### Stack

- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Security (session + CSRF)
- Spring Data JPA
- Liquibase
- PostgreSQL
- Maven Wrapper

### Architecture

Layered pattern:
- controller: HTTP entrypoints
- service: business rules
- repository: data access
- model: JPA entities
- dto: request/response contracts
- validation: custom validations
- exception: centralized error handling

### Local setup

1. Start PostgreSQL with Docker Compose:

```bash
docker-compose up -d
```

2. Run API:

```bash
./mvnw spring-boot:run
```

On Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Default API URL: `http://localhost:8080`

### Configuration and environment

In `src/main/resources/application.properties`:
- datasource: `jdbc:postgresql://localhost:5432/postgres`
- default db credentials: `postgres/postgres`
- session cookie name: `GAMELOG_SESSION`

Optional env vars:
- `APP_SECURITY_ALLOWED_ORIGINS`
- `APP_SECURITY_COOKIE_SECURE`
- `OPENAI_API_KEY` (for AI-related evolution)

### Authentication (important)

This API uses HTTP session + CSRF (not JWT).

Recommended flow:
1. `GET /auth/csrf` to retrieve CSRF token
2. `POST /users` to sign up
3. `POST /auth/login` with `identifier` (email or username) and `password`
4. send session cookie + CSRF header in write requests

### Current endpoints

Authentication:
- `GET /auth/csrf` (public)
- `POST /auth/login` (public)
- `POST /auth/logout` (authenticated)
- `GET /auth/me` (authenticated)

Users:
- `POST /users` (public)
- `GET /users/{id}` (authenticated)
- `PUT /users/{id}` (authenticated)
- `DELETE /users/{id}` (admin)

Games:
- `GET /games/{id}` (authenticated)
- `DELETE /games/{id}` (authenticated)

Genres:
- `GET /genres/{id}` (authenticated)

Game-genre:
- `GET /game-genres/{id}` (authenticated)

Favorites:
- `POST /favorites` (authenticated)
- `GET /favorites/{id}` (authenticated)
- `DELETE /favorites/{id}` (authenticated)

Ratings:
- `POST /ratings` (authenticated)
- `GET /ratings/{id}` (authenticated)
- `PUT /ratings/{id}` (authenticated)
- `DELETE /ratings/{id}` (authenticated)

### Sample payloads

Create user (`POST /users`):

```json
{
  "email": "user@example.com",
  "username": "user123",
  "password": "StrongPass123",
  "avatarUrl": "https://example.com/avatar.png",
  "bio": "My gamer profile"
}
```

Login (`POST /auth/login`):

```json
{
  "identifier": "user@example.com",
  "password": "StrongPass123"
}
```

Create favorite (`POST /favorites`):

```json
{
  "userId": 1,
  "gameId": 2
}
```

Create rating (`POST /ratings`):

```json
{
  "userId": 1,
  "gameId": 2,
  "score": 5,
  "review": "Excellent game"
}
```

### Tests and build

```bash
./mvnw test
./mvnw package
```

Windows (PowerShell):

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

### Developer guide

Check `CONTRIBUTING.md` for the full guide on adding new entities, controllers, and other backend components.

