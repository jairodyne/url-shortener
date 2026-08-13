# Encurtador de URL

Desafio técnico Java: sistema web que recebe uma URL, retorna uma URL encurtada e redireciona ao acessar o código.

## Stack
- Java 8, WildFly 10, JAX-RS 2.0, CDI 1.2, JPA 2.1 (Hibernate), H2 (em memória)
- Frontend Angular (build embutido no WAR)

## Estrutura
- `src/main/java` — API, serviço, repositório, modelo (camadas isoladas)
- `src/main/resources/META-INF/persistence.xml` — persistência H2
- `frontend/` — aplicação Angular
- `Dockerfile` / `docker-compose.yml` — execução em container
- `.github/workflows/ci.yml` — pipeline de CI

> Documentação completa (como rodar, decisões de design, trade-offs) será adicionada ao final.
