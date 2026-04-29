Use estas instrucoes para trabalhar no backend do Gamelog.

Contexto do projeto
- Este e um backend Spring Boot 4.0.3 com Java 21, Maven Wrapper, PostgreSQL, Liquibase e Spring Security baseado em sessao.
- A documentacao e a comunicacao tecnica devem priorizar PT-BR.

Regras de implementacao
- Siga a arquitetura em camadas: controller, service, repository, model, dto, validation e exception.
- Prefira DTOs para entrada e saida de dados; nao exponha entidades JPA diretamente nas APIs.
- Preserve o fluxo de autenticacao por sessao + CSRF; nao introduza JWT sem pedido explicito.
- Respeite os padroes de seguranca existentes, incluindo cookie de sessao, CORS baseado em ambiente e rotas publicas/autenticadas ja definidas.
- Use Liquibase para evolucao de schema e mantenha as alteracoes de banco consistentes com os changelogs existentes.
- Considere os limites numericos do dominio, especialmente campos de avaliacao que precisam caber em numeric(3,2).
- Mantenha credenciais, chaves e URLs sensiveis em variaveis de ambiente; nao hardcode segredos em application.properties.

Codigo e estilo
- Siga os padroes de nomenclatura e organizacao ja presentes no projeto.
- Quando adicionar novas features, atualize testes relevantes e, se necessario, a documentacao tecnica.
- Evite refatoracoes amplas fora do escopo da tarefa.
- Preserve o comportamento publico existente, principalmente contratos de endpoint, validacoes e tratamento padronizado de erros.

Integracoes
- Quando houver trabalho com email, use a configuracao SMTP existente e leia credenciais de variaveis de ambiente.
- Quando houver trabalho com recomendacoes ou IA, respeite a dependencia Spring AI/OpenAI e nao exponha endpoints ou prompts novos sem contexto da tarefa.
- Para jogos, generos, favoritos e avaliacoes, siga os relacionamentos e regras de negocio ja implementados.

Antes de concluir uma alteracao
- Verifique se a mudanca compila com o padrao atual do projeto.
- Confirme se nao quebrou seguranca, migracoes ou contratos de API.
- Se a tarefa tocar dados persistidos, revise impacto em entidades, repositorios, services e changelogs.