# Gamelog API

API backend do Gamelog para autenticação, catálogo de jogos, favoritos, avaliações, recuperação de senha e integrações com serviços externos.

## Visão geral

O projeto centraliza o fluxo de descoberta e acompanhamento de jogos em uma API Spring Boot. Hoje o backend já entrega:

- autenticação por sessão com CSRF;
- cadastro e gestão de usuários;
- consulta de jogos, gêneros e relacionamento jogo-gênero;
- favoritos;
- avaliações com nota e comentário;
- recuperação e redefinição de senha por e-mail;
- integrações com RAWG, serviço de tradução, envio de e-mail e base para OpenAI.

## Stack

- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Spring Validation
- Liquibase
- Spring Mail
- Spring AI 2.0.0-M2
- PostgreSQL
- Maven Wrapper
- JaCoCo

## Estrutura do projeto

O código segue uma arquitetura em camadas:

- controllers em `src/main/java/com/gamelog/gamelog/controller`
- DTOs em `src/main/java/com/gamelog/gamelog/controller/dto`
- services em `src/main/java/com/gamelog/gamelog/service`
- repositories em `src/main/java/com/gamelog/gamelog/repository`
- entities em `src/main/java/com/gamelog/gamelog/model`
- validações em `src/main/java/com/gamelog/gamelog/validation`
- exceções em `src/main/java/com/gamelog/gamelog/exception`

Também existem recursos e utilitários importantes em:

- `src/main/resources/liquibase` para migrations
- `scripts/generate_games_seed_sql.py` para geração de seed
- `Video Games Data.csv` como base de dados de jogos
- `banco/` para material visual do schema

## Como rodar localmente

1. Suba o banco com Docker Compose:

```bash
docker-compose up -d
```

2. Rode a API:

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A API sobe por padrão em `http://localhost:8080`.

## Configuração

As configurações principais ficam em `src/main/resources/application.properties`.

### Banco de dados

- URL padrão: `jdbc:postgresql://localhost:5432/postgres`
- usuário padrão: `postgres`
- senha padrão: `postgres`
- migrations: `classpath:liquibase/master.xml`

### Sessão e segurança

- cookie de sessão: `GAMELOG_SESSION`
- timeout de sessão: `30m`
- CSRF habilitado para requisições de escrita
- CORS controlado por `APP_SECURITY_ALLOWED_ORIGINS`
- cookie seguro opcional via `APP_SECURITY_COOKIE_SECURE`

### E-mail e redefinição de senha

- SMTP: `smtp.gmail.com:587`
- credenciais via `MAIL_USERNAME` e `MAIL_APP_PASSWORD`
- remetente opcional via `MAIL_FROM`
- o link de redefinição usa `APP_FRONTEND_BASE_URL` e envia o token por query string

### Integrações externas

- `OPENAI_API_KEY` para recursos planejados de IA
- `RAWG_API`, `RAWG_API_BASE_URL`, `RAWG_API_TIMEOUT_MS`, `RAWG_API_MIN_INTERVAL_MS`
- `APP_TRANSLATION_BASE_URL`, `APP_TRANSLATION_TIMEOUT_MS`, `APP_TRANSLATION_CONNECT_TIMEOUT_MS`, `APP_TRANSLATION_RETRY_COUNT`, `APP_TRANSLATION_SOURCE_LANG`, `APP_TRANSLATION_TARGET_LANG`
- `VGCHARTZ_BASE_URL` e `APP_IMAGE_DEFAULT_URL`

### Observação importante

Campos numéricos como `average_rating` e `default_rating` usam escala `numeric(3,2)` no banco. Seeds e importações devem respeitar limite de `9.99` para evitar arredondamento para `10.00`.

## Autenticação

Este backend usa sessão HTTP + CSRF, não JWT.

Fluxo recomendado:

1. `GET /auth/csrf` para obter o token CSRF.
2. `POST /users` para criar conta ou `POST /auth/login` para entrar.
3. Enviar cookie de sessão e o header CSRF nas requisições de escrita.
4. `POST /auth/logout` encerra a sessão.

## Endpoints atuais

### Auth

- `GET /auth/csrf` - público
- `POST /auth/login` - público
- `POST /auth/logout` - autenticado
- `GET /auth/me` - autenticado
- `POST /auth/forgot-password` - público
- `POST /auth/reset-password` - público

### Users

- `POST /users` - público
- `GET /users/{id}` - autenticado
- `PUT /users/{id}` - autenticado
- `DELETE /users/{id}` - admin

### Games

- `GET /games/explore` - catálogo paginado com filtros
- `GET /games/popular` - jogos populares
- `GET /games/top-rated` - jogos mais bem avaliados
- `GET /games/{id}` - consulta por id
- `GET /games/slug/{slug}` - consulta por slug
- `DELETE /games/{id}` - manutenção/admin

### Genres

- `GET /genres` - lista de gêneros
- `GET /genres/{id}` - consulta por id

### Game-genre

- `GET /game-genres/{gameId}/{genreId}` - relacionamento entre jogo e gênero

### Favorites

- `POST /favorites`
- `GET /favorites/{id}`
- `DELETE /favorites/{id}`

### Ratings

- `POST /ratings`
- `GET /ratings/{id}`
- `PUT /ratings/{id}`
- `DELETE /ratings/{id}`

## Seed e dados

Para gerar carga inicial de jogos, o repositório inclui o CSV `Video Games Data.csv` e o script `scripts/generate_games_seed_sql.py`.

Se a intenção for criar ou atualizar seeds, vale revisar também as migrations Liquibase antes de importar novos registros.

## Testes e build

```bash
./mvnw test
./mvnw package
./mvnw clean compile
```

No Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd clean compile
```

O relatório do JaCoCo é gerado em `target/site/jacoco`.

## Documentação adicional

- `CONTRIBUTING.md` descreve o padrão para adicionar novas entidades, services, controllers, migrations e testes.
- `HELP.md` reúne referências rápidas do Maven e do Spring.
