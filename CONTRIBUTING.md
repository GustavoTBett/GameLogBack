# Contributing Guide

## PT-BR

### Objetivo

Este guia explica como criar e evoluir recursos no backend do Gamelog seguindo o padrao do projeto.

Escopo principal:
- nova entidade
- novo repository
- novo service
- novo controller
- novo DTO
- nova migration Liquibase
- ajustes de seguranca e testes

### Estrutura atual do backend

Pacotes principais:
- `model`: entidades JPA
- `repository`: interfaces Spring Data
- `service/<dominio>`: interfaces e implementacoes de regra de negocio
- `controller`: endpoints REST
- `controller/dto`: contratos de request/response
- `validation/<dominio>`: validacoes customizadas
- `exception`: tratamento de erros

### Passo a passo para criar um novo recurso

Exemplo de dominio: `Comment` (comentario em jogo).

1. Criar entidade em `model`
- escolha heranca correta:
- use `MasterEntity` para id/version
- use `MasterEntityWAudit` para id/version + createdAt/updatedAt
- adicione validacoes com Jakarta Validation (`@NotBlank`, `@NotNull`, etc.)
- configure relacoes JPA (`@ManyToOne`, `@JoinColumn`, etc.)

2. Criar repository em `repository`
- estenda `JpaRepository<Entidade, Long>`
- adicione consultas customizadas apenas se necessario

3. Criar service interface e implementacao
- interface em `service/<dominio>/<Entidade>Service.java`
- implementacao em `service/<dominio>/<Entidade>ServiceImpl.java`
- mantenha regras de negocio no service, nao no controller
- padrao recomendado: `save`, `get`, `delete`, e metodos de validacao/conversao de DTO quando aplicavel

4. Criar DTOs em `controller/dto`
- prefira DTO para entrada de dados (request)
- valide DTO com anotacoes de Jakarta Validation
- no controller, use `@Valid` no corpo da requisicao

5. Criar controller em `controller`
- use `@RestController` e `@RequestMapping("/recurso")`
- injete dependencias por construtor
- retorne `ResponseEntity`
- para criacao, retorne `201 Created` com header `Location`
- para busca por id, retorne `404 Not Found` quando ausente

6. Atualizar seguranca em `SecurityConfig`
- decidir se rota e publica, autenticada ou admin
- atualizar `authorizeHttpRequests` quando necessario
- para operacoes de escrita, lembrar que CSRF esta habilitado

7. Criar migration Liquibase
- criar SQL na pasta de changelog (sugestao: novo bloco de versao, ex.: `GLBACK-002`)
- criar/atualizar `changelog.xml` do bloco
- incluir novo changelog em `src/main/resources/liquibase/master.xml`
- manter migrations idempotentes e rastreaveis

8. Tratar erros de negocio
- quando necessario, criar excecoes especificas em `exception/<dominio>`
- padronizar retorno com o handler global ja existente

9. Criar ou atualizar testes
- controller tests para contratos HTTP
- service tests para regras de negocio
- garanta cenarios felizes e de erro

### Convencoes recomendadas

- Nomes de classes por dominio: `FavoriteService`, `FavoriteServiceImpl`, `FavoriteRepository`
- Nomes de rota no plural: `/favorites`, `/ratings`
- Evite logica de negocio dentro do controller
- Evite expor entidade diretamente em entradas complexas; prefira DTO
- Mantenha metodos pequenos e com responsabilidade unica

### Checklist de conclusao (Definition of Done)

Antes de abrir PR:
- entidade/model criada com validacoes
- repository criado
- service + implementacao criados
- DTOs criados/atualizados
- controller criado/atualizado
- seguranca revisada
- migration Liquibase criada e registrada no `master.xml`
- testes criados/atualizados
- build e testes executando localmente
- README atualizado se houver mudanca de comportamento de API

### Comandos uteis

Executar API:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Rodar testes:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Subir banco com Docker:

```bash
docker-compose up -d
```

---
