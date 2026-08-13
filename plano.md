# Plano — Encurtador de URL em Java (WildFly 10)

## 1. Objetivo
Sistema web que recebe uma URL, retorna uma URL encurtada e redireciona ao acessar o código.
Requisito crítico: o motor de geração processa **uma requisição por vez, de forma sincronizada**.

## 2. Decisões de arquitetura (confirmadas)
| Tema | Decisão | Justificativa |
|---|---|---|
| Motor síncrono | `synchronized` em CDI `@ApplicationScoped` | Serializa geração+persistência, testável, atende requisito |
| Persistência | JPA+Hibernate sobre **H2 em memória** | Zero dependência externa; demo imediata no WildFly |
| Frontend | **Angular** mínimo (build copiado ao WAR) | Requisito "Angular ou React"; deploy único |
| CI/CD | **Makefile local + GitHub Actions** | Pipeline "mesmo local" + prova remota |
| Short URL | `…/r/{code}` | Evita servlet catch-all que quebraria assets estáticos do Angular |
| Redirecionamento | HTTP **302** | Evita cache de navegador; preserva contagem de cliques |
| Versionamento | **GitFlow** em repositório **GitHub** | Padrão de trabalho definido no desafio |

## 3. Stack e versões (Java 8 / WildFly 10)
- Java 8 (`source/target 1.8`), Maven 3.6.3, WAR
- Dependências **provided**: JAX-RS 2.0 (javax.ws.rs 2.0.1), CDI 1.2, JPA 2.1, Servlet 3.1
- Runtime: `jboss/wildfly:10.1.0.Final`
- Testes: JUnit 4.12, Mockito 2.x, H2 (teste JPA sem WildFly), REST Assured 3.x
- Angular 18 (Node via `frontend-maven-plugin`), JPA/Hibernate com `hibernate.hbm2ddl.auto=create`

## 4. Estrutura do projeto
```
url/
├── README.md
├── Makefile                     # test / build / docker-up / docker-down / ci
├── .github/workflows/ci.yml
├── docker-compose.yml
├── Dockerfile                   # maven:3.6.3-jdk-8 → jboss/wildfly:10.1.0.Final
├── pom.xml
├── frontend/                    # app Angular (mínimo)
└── src/main/
    ├── java/.../urlshortener/
    │   ├── bootstrap/JaxRsApplication.java        # @ApplicationPath("/api")
    │   ├── api/UrlShortenerResource.java          # POST /api/urls
    │   ├── api/RedirectResource.java              # GET /r/{code} → 302
    │   ├── api/exception/ErrorMapper.java         # 400/404/409
    │   ├── service/UrlShortenerService.java       # fluxo completo sincronizado
    │   ├── service/CodeGenerator.java             # motor synchronized + base62
    │   ├── service/UrlValidator.java              # scheme http/https, tamanho
    │   ├── repository/UrlRepository.java          # JPA
    │   ├── model/UrlEntity.java                   # UNIQUE em code e alias
    │   └── model/dto/ (CreateUrlRequest, CreateUrlResponse, ErrorResponse)
    ├── resources/META-INF/persistence.xml         # H2 + hbm2ddl=create
    └── webapp/ (dist do Angular copiado no build)
```

## 5. Contrato da API
- `POST /api/urls` — corpo `{"url":"…","alias":"opcional"}`
  - sem alias → código base62 automático (6–7 chars)
  - com alias → checagem de disponibilidade
  - `201` → `{shortUrl, code, url}` | `400` inválida | `409` alias ocupado
- `GET /r/{code}` → `302 Location: original` | `404` inexistente
- Unicidade: bloco `synchronized` (serializa) + constraints `UNIQUE` (backstop)

## 6. Garantia de qualidade mínima

### 6.1 Testes automatizados (unitários e integração)
| Camada | Tipo | O que cobre |
|---|---|---|
| `CodeGenerator` | Unitário | geração base62, tamanho, unicidade, colisão→retry |
| `UrlValidator` | Unitário | URL válida/inválida, scheme http/https, tamanho, alias inválido |
| `UrlShortenerService` | Unitário (Mockito) | fluxo sem alias, alias ocupado→409, resolve conta clique, exceções mapeadas |
| `UrlRepository` + JPA | Integração (H2 real) | salvar/buscar, `UNIQUE` em code e alias viola constraint |
| Motor concorrente | Unitário/integração | N threads simultâneas → nenhuma duplicidade (prova do requisito síncrono) |
| Camada REST | Integração (REST Assured) | 201/400/409, 302 com `Location`, 404, corpo de erro JSON |

### 6.2 Metas de cobertura (Jacoco)
- **Obrigatório**: `service/*` e `CodeGenerator` ≥ **80%** de cobertura de linha.
- **Desejável**: `repository/*` ≥ 70% (via teste de integração H2).
- **Verificação**: `jacoco-maven-plugin` gera relatório em `target/site/jacoco/`; CI falha se meta não atingida (`check` no aggregate).
- Checklist de aceite de cada parte: testes de **sucesso**, **falha**, e **limite/edge** (ex.: URL sem scheme, alias repetido, code inexistente, string vazia).

### 6.3 Clareza
- Nomes expressivos; 1 responsabilidade por classe; exceções de domínio explícitas.
- DTOs isolando a API da entidade; contrato REST documentado no README com `curl`.
- Sem comentários supérfluos; código auto-explicativo.

### 6.4 Manutenibilidade
- Camadas isoladas (api → service → repository), sem vazamento de JPA para o REST.
- `CodeGenerator` separado do serviço → troca de algoritmo sem tocar o fluxo.
- Configuração centralizada (`persistence.xml`, properties de base URL).
- Regras de negócio em serviços, não na camada web → testáveis sem container.
- CI rodando testes a cada push (proteção de `develop` e `main`).

## 7. Versionamento — GitFlow + GitHub

### 7.1 Estrutura de branches
```
main        (produção, protegida — sem push direto)
 ├── develop                 (integração, protegida)
 │    ├── feature/*          (cada Parte 0..7 vira uma ou mais feature)
 │    ├── fix/*              (correções)
 │    └── release/v1.0.0     (preparo de release)
 └── hotfix/*                (emergências em produção)
```

### 7.2 Convenções
- **Branches por parte**: `feature/p1-esqueleto-maven`, `feature/p2-persistencia`, ... e `feature/p6-docker-ci`.
- **Commits atômicos** e mensagens descritivas: `feat: gera código base62 no motor síncrono`, `test: cobre colisão de alias`, `docs: descreve API no README`.
- **Pull Requests** para `develop` com descrição do que foi feito e como testar; revisão antes do merge (squash).
- **Release**: ao fim de todas as partes, branch `release/v1.0.0` → merge em `main` (tag `v1.0.0`) e volta a `develop`.
- **GitHub**: repositório remoto + **Actions** rodando o pipeline na branch `develop` a cada push; `main` exige passagem do CI.

### 7.3 Proteção de branches (Settings → Branches → Branch protection rules)
| Regra | `main` | `develop` |
|---|---|---|
| Require pull request review | Sim (1 aprovação) | Sim (1 aprovação) |
| Require status check (CI) | Sim (`build-and-test`) | Sim (`build-and-test`) |
| Require branches up to date | Sim | Sim |
| No force push / no deletions | Sim | Sim |
| Enforce admins | Sim | Não |

---

## PARTES DE EXECUÇÃO

### Parte 0 — Inicialização do repositório (GitFlow + GitHub)
**Tarefas**
- [ ] `git init`, branch inicial `develop`, criar `main`
- [ ] Push para repositório **GitHub** privado
- [ ] `README.md` esqueleto (objetivo + estrutura) e `.gitignore` (target/, node_modules/, dist/, .idea/, *.iml)
- [ ] Proteção de branches (`develop`, `main`) conforme 7.3
**Verificar** — checkout `develop` limpo no remoto; `.gitignore` evita artefatos de build

### Parte 1 — Esqueleto Maven + Java 8  (`feature/p1-esqueleto-maven`)
**Tarefas**
- [ ] `pom.xml`: group/artifact, packaging `war`, `maven.compiler.source/target=1.8`
- [ ] Dependências `provided`: javax.ws.rs-api 2.0.1, javax.enterprise (cdi-api 1.2), javaee-api (jpa/servlet 2.1/3.1) — ou per-PI para evitar conflito
- [ ] `frontend-maven-plugin` (1.12.x) + `maven-war-plugin` (copia `frontend/dist` para webapp)
- [ ] Surefire + JUnit 4.12 + Mockito + H2 + REST Assured + Jacoco (metas 6.2)
- [ ] `JaxRsApplication.java` vazio (`@ApplicationPath("/api")`)
**Verificar** — `mvn -q test` passa; `mvn -q package` gera `url-shortener.war`

### Parte 2 — Modelo + Persistência  (`feature/p2-persistencia`)
**Tarefas**
- [ ] `UrlEntity`: `id` (long auto), `code` (unique), `alias` (unique, nullable), `originalUrl` (não-nulo), `createdAt`, `clickCount`
- [ ] `persistence.xml` (H2 mem, JPA, `hbm2ddl=create`)
- [ ] `UrlRepository`: `save`, `findByCode`, `findByAlias`, `incrementClicks`
**Verificar** — teste de integração JPA (H2 em JVM): salvar/buscar, duplicidade de code/alias lança `ConstraintViolationException`

### Parte 3 — Motor síncrono + Serviço  (`feature/p3-motor-servico`)
**Tarefas**
- [ ] `CodeGenerator` (`@ApplicationScoped`): contador atômico → base62; método `synchronized generateUniqueCode()` com loop até não colidir
- [ ] `UrlValidator`: valida URL (scheme http/https, tamanho máx. 2048), alias (charset seguro, máx. 30)
- [ ] `UrlShortenerService` (`@ApplicationScoped`): `createUrl` **inteiro `synchronized`** (valida→gera/verifica alias→persiste; alias ocupado→exceção; colisão de constraint→retry), `resolve(code)` (incrementa cliques), `buildShortUrl(base, code)`
- [ ] Exceções de domínio: `InvalidUrlException`, `AliasAlreadyExistsException`, `UrlNotFoundException`
**Verificar** — unit tests (Mockito): geração única, colisão→retry, alias ocupado→erro, resolve conta clique; teste de concorrência (N threads) confirmando nenhuma duplicidade

### Parte 4 — Camada REST  (`feature/p4-rest`)
**Tarefas**
- [ ] `UrlShortenerResource`: `POST /api/urls` → 201/400/409
- [ ] `RedirectResource`: `GET /r/{code}` → 302 (monta `base` a partir do request) / 404
- [ ] `ErrorMapper` (exception → JSON `{status, message}`)
- [ ] Deploy manual inicial no WildFly (via Docker) para validar scopes `provided` — risco de conflito de APIs
**Verificar** — testes REST Assured + `curl` contra app no Docker: criar sem/com alias, 409 em alias duplicado, 302 com `Location` correto, 404

### Parte 5 — Frontend Angular  (`feature/p5-frontend`)
**Tarefas**
- [ ] App mínimo: campo URL, campo alias opcional, botão "Gerar"
- [ ] Exibe short URL + botão copiar; mensagens de erro (400/409)
- [ ] `proxy.conf.json` (dev aponta `/api` para `localhost:8080/url-shortener/api`)
- [ ] Base href relativa para deploy dentro do WAR; integração com `frontend-maven-plugin`
**Verificar** — `mvn package` gera WAR com o SPA; smoke test manual no navegador (gerar → copiar → abrir redireciona)

### Parte 6 — Docker + CI/CD  (`feature/p6-docker-ci`)
**Tarefas**
- [ ] `Dockerfile` multi-stage (maven:3.6.3-jdk-8 build → `jboss/wildfly:10.1.0.Final` copia WAR)
- [ ] `docker-compose.yml` (serviço `web` porta 8080)
- [ ] `Makefile`: `test`, `build`, `docker-up`, `docker-down`, `ci` (test→package→docker-up→smoke curl)
- [ ] `.github/workflows/ci.yml`: checkout → Java 8 → `mvn test` → `mvn package` → build imagem → smoke
**Verificar** — `make ci` roda de ponta a ponta localmente; workflow passa no GitHub (PR/develop)

### Parte 7 — README + Finalização  (`feature/p7-readme` + `release/v1.0.0`)
**Tarefas**
- [ ] README: como rodar (local Maven + Angular dev; Docker), como testar, exemplo `curl`
- [ ] Decisões de design e trade-offs (seção 2 + seção 8)
- [ ] Qualidade: testes executados, cobertura, como garantir manutenibilidade
- [ ] "O que faria diferente com mais tempo"
- [ ] Criar `release/v1.0.0` → merge em `main` + tag `v1.0.0` → volta a `develop`
**Verificar** — um dev novo consegue subir o projeto seguindo o README em < 15 min; `main` tagged `v1.0.0` com CI verde

## 8. Trade-offs documentados no README
- **Prefixo `/r/`** vs URL "raiz limpa" (conflito servlet/static no WildFly 10)
- **`synchronized` intra-JVM** vs fila JMS/lock distribuído — suficiente para single-node; JMS da stack **não usado** no fluxo principal (latência/complexidade), citado como evolução
- **H2 em memória** perde dados ao reiniciar → produção: Postgres
- **302 vs 301**: 302 evita cache e preserva métricas
- **base62 determinístico + retry** dentro do bloco sincronizado

## 9. Riscos
- Imagem legada `jboss/wildfly:10.1.0.Final` (2016) — sem patches; aceitável para o desafio
- Angular moderno exige Node 18+; `frontend-maven-plugin` baixa Node no build (precisa de internet)
- Conflito de APIs `javaee-api` no WildFly → usar scopes `provided` e testar deploy cedo (Parte 4)
- GitFlow em projeto solo pode parecer overhead — mantido por ser exigência do desafio

## 10. Fora de escopo (se sobrar tempo, nesta ordem)
- `GET /api/urls/{code}` com contagem de cliques
- TTL/expiração de URLs
- Postgres real via docker-compose
- JMS no fluxo principal (auditoria/decoupling)
