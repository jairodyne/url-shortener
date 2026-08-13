# Encurtador de URL

Sistema web que recebe uma URL, retorna uma URL encurtada e redireciona ao acessar o código. Desafio técnico Java.

Requisito crítico: o motor de geração processa **uma requisição por vez, de forma sincronizada**.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 8, JAX-RS 2.0, CDI 1.2, JPA 2.1 (Hibernate 5.6) |
| Servidor | WildFly 10.1.0.Final |
| Persistência | H2 em memória (sem dependência externa) |
| Frontend | Angular 18 (build embutido no WAR) |
| Testes | JUnit 4, Mockito, REST Assured, H2 (integração JPA) |
| CI/CD | Makefile + GitHub Actions (job `build-and-test`) |

## Funcionalidades

- Encurtar uma URL: gera código curto automático (base62) ou usa um **alias** personalizado
- Redirecionar `/r/{code}` para a URL original com HTTP 302
- Verificar disponibilidade de alias (409 se ocupado), validar URL (400 se inválida)
- Frontend Angular: campo URL + alias opcional, botão "Encurtar", copiar resultado, mensagens de erro

## Contrato da API

### `POST /api/urls` — criar URL encurtada

```bash
# Sem alias (código base62 automático)
curl -X POST http://localhost:8080/url-shortener/api/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/pagina-longa"}'
# 201 → {"shortUrl":"http://localhost:8080/url-shortener/r/000001","code":"000001","url":"https://example.com/pagina-longa"}

# Com alias personalizado
curl -X POST http://localhost:8080/url-shortener/api/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com","alias":"meu-link"}'
# 201 → {"shortUrl":"http://localhost:8080/url-shortener/r/meu-link","code":"meu-link","url":"https://example.com"}

# Erros
# 400 — URL inválida (sem http/https, sem host, vazia, > 2048 chars) ou alias inválido
# 409 — alias já em uso
# 500 — falha inesperada (resposta JSON {status, message})
```

### `GET /r/{code}` — redirecionar

```bash
curl -i http://localhost:8080/url-shortener/r/meu-link
# 302 → Location: https://example.com
# 404 → código inexistente
```

### Validações

- URL: deve começar com `http://` ou `https://`, ter host, no máximo 2048 caracteres
- Alias: `[a-zA-Z0-9_-]`, no máximo 30 caracteres, não pode estar em uso

## Como rodar

### Docker (recomendado)

```bash
make docker-up     # constrói imagem e sobe WildFly na porta 8080
# Acesse http://localhost:8080/url-shortener/
make docker-down   # derruba o container
```

A imagem é multi-stage: build com `maven:3.6.3-jdk-8` → runtime `jboss/wildfly:10.1.0.Final`.

### Local com Maven (Java 8)

```bash
mvn package        # gera target/url-shortener.war (inclui o frontend)
# Copie o WAR para $WILDFLY_HOME/standalone/deployments/ e inicie o WildFly 10
```

O build usa o plugin `frontend-maven-plugin`, que baixa o Node 20.11.0 (precisa de internet na primeira vez).

### Frontend em modo dev (hot reload)

```bash
cd frontend
npm install
npm start          # Angular dev server em http://localhost:4200
```

O `proxy.conf.json` encaminha `/api` para `http://localhost:18080/url-shortener` — rode o WildFly nessa porta ou ajuste o target do proxy conforme sua instalação.

## Como testar

```bash
make test    # testes unitários (surefire) — 30 testes
make it      # testes de integração REST (REST Assured) contra o container — 5 testes
make ci      # pipeline completo: test → build → docker-up → smoke → it → docker-down
make smoke   # valida o deploy com curl (criar URL + redirecionar 404)
```

### Cobertura

```bash
mvn test jacoco:report   # relatório em target/site/jacoco/index.html
```

| Camada | Cobertura de linha (unitários) |
|---|---|
| `service/*` (`CodeGenerator`, `UrlValidator`, `UrlShortenerService`) | **87,5%** (meta ≥ 80%) |
| `repository/*` (`UrlRepository`) | **86,0%** (meta ≥ 70%) |

A camada REST (resource, servlet de redirecionamento, mappers) é exercitada pelos testes de integração REST Assured (`UrlShortenerApiIT`), que validam os códigos 201/400/404/409/302.

## Estrutura do projeto

```
url/
├── Makefile                     # test / build / docker-up / docker-down / smoke / it / ci
├── Dockerfile                   # build maven → jboss/wildfly:10.1.0.Final
├── docker-compose.yml           # serviço web na porta 8080
├── .github/workflows/ci.yml     # pipeline build-and-test
├── scripts/smoke.sh             # smoke test com curl
├── pom.xml
├── frontend/                    # app Angular (build embutido no WAR)
└── src/main/
    ├── java/.../urlshortener/
    │   ├── bootstrap/JaxRsApplication.java        # @ApplicationPath("/api")
    │   ├── api/UrlShortenerResource.java          # POST /api/urls
    │   ├── api/RedirectServlet.java               # GET /r/{code} → 302
    │   ├── api/exception/*Mapper.java             # 400/409 → JSON {status, message}
    │   ├── service/UrlShortenerService.java       # fluxo completo sincronizado
    │   ├── service/CodeGenerator.java             # motor synchronized + base62
    │   ├── service/UrlValidator.java              # validações de URL e alias
    │   ├── repository/UrlRepository.java          # JPA
    │   └── model/ (+ dto/)                        # UrlEntity e DTOs da API
    ├── resources/META-INF/persistence.xml         # H2 + hbm2ddl=create
    └── webapp/ (beans.xml + assets do Angular)
```

## Decisões de design e trade-offs

| Tema | Decisão | Trade-off |
|---|---|---|
| Motor síncrono | `synchronized` em CDI `@ApplicationScoped` | Serializa geração+persistência e atende o requisito; não escala para multi-node (evolução: fila/lock distribuído) |
| Persistência | JPA/Hibernate sobre H2 em memória | Zero dependência externa e demo imediata; **dados se perdem ao reiniciar** → produção: Postgres |
| Rota curta | `/r/{code}` (servlet, fora de `/api`) | Mantém short URLs limpas sem conflitar com os assets estáticos do Angular na raiz do WAR |
| Redirecionamento | HTTP **302** | Evita cache do navegador e preserva a contagem de cliques (301 reusa cache) |
| Código curto | base62 determinístico + retry | Determinístico e testável; a unicidade é garantida pelo bloco sincronizado + constraint `UNIQUE` (backstop) |
| Frontend | Angular mínimo embutido no WAR via `frontend-maven-plugin` | Deploy único (WAR); build precisa de internet para baixar o Node |
| CI/CD | Makefile local + GitHub Actions | Mesmo pipeline rodando local e no remoto; `main`/`develop` exigem o check `build-and-test` |

## Qualidade e manutenibilidade

- **Camadas isoladas**: `api → service → repository`, sem vazamento de JPA para o REST (DTOs isolam a entidade)
- **1 responsabilidade por classe**: `CodeGenerator`, `UrlValidator` e `UrlShortenerService` separados → algoritmo trocável sem tocar o fluxo
- **Regras de negócio em serviços** (não na camada web) → testáveis sem container
- **Exceções de domínio explícitas**: `InvalidUrlException`, `AliasAlreadyExistsException`, `UrlNotFoundException` mapeadas para 400/409/404
- **Configuração centralizada**: `persistence.xml`, base URL montada a partir do request
- **Testes de sucesso, falha e edge** em cada componente; teste de concorrência (N threads) prova que não há duplicidade no motor síncrono
- **CI a cada push** (test → package → Docker → smoke → ITs) protegendo `develop` e `main`

## O que faria diferente com mais tempo

1. **Cobertura consolidada no CI**: mesclar `jacoco.exec` de unitários + integração e aplicar `jacoco:check` para falhar o build abaixo das metas
2. **`GET /api/urls/{code}`** com contagem de cliques (o contador já existe na entidade)
3. **TTL/expiração de URLs** (limpeza de registros antigos)
4. **Postgres real** via docker-compose, com migration (Flyway/Liquibase) em vez de `hbm2ddl=create`
5. **Escala do motor**: em multi-node, trocar `synchronized` intra-JVM por uma fila/lock distribuído (ex.: JMS da própria stack ou Redis) — a API de serviço não mudaria
6. **Segurança/observabilidade**: rate limiting, métricas (prometheus), logs estruturados
7. **Frontend**: testes e2e (Playwright), validação de URL no cliente, tema responsivo