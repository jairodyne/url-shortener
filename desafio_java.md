# Desafio Técnico JAVA

---

## Orientações Gerais

Esforço esperado
Você terá 2 dias corridos de prazo.
O desafio foi pensado para ser feito em até 6 horas de esforço total.
Não esperamos refino visual, deploy em cloud ou features extras — mantenha foco no essencial.
Não buscamos avaliar se você ‘sabe programar’, mas sim sua visão de sênior: clareza de arquitetura, trade-offs e comunicação.

Objetivo
Queremos conhecer como você pensa e estrutura soluções, não apenas se sabe “fazer código rodar”.
O desafio é simples por natureza, mas o que nos interessa é:
Como você organiza o projeto (estrutura de pastas, camadas, módulos).
Como você toma decisões técnicas e documenta trade-offs.
Como você garante qualidade mínima (testes, clareza, manutenibilidade).
Como você comunica suas escolhas (via código e README).


---

## O Desafio

Fluxo de Operação Resumido:
Usuário acessa a interface e digita a URL original.
  - Pode também informar um alias (nome curto personalizado).
  - Clica no botão de gerar.
O sistema processa a solicitação
  - Se tiver alias, verifica se está disponível.
  - Se não tiver, cria automaticamente um código curto.
O sistema retorna a nova URL encurtada
  - Essa URL é exibida para o usuário.
  - O usuário pode copiar e compartilhar.
Quando alguém acessa a URL encurtada
  - O sistema identifica a URL original correspondente.
  - Redireciona o usuário para a URL original.

Problema a resolver
Desenvolver um sistema web que receba uma URL digitada pelo usuário e retorne uma URL encurtada, que ao ser acessada, deverá fazer o redirecionamento para URL original.

---

## Requisitos

Requisitos opcionais (não obrigatórios, mas contam pontos) :
Front-end simples) para consumir a API.
Testes automatizados (unitários e/ou integração).
Uso de container (Docker).
Pipeline simples de CI/CD (mesmo local).

Requisitos mínimos
Back-end em Java
  - O motor de geração deve ser capaz de processar apenas uma requisição por vez, de forma sincronizada.
API REST simples para consumir as funcionalidades.
Persistência opcional (pode ser em memória, banco relacional ou NoSQL).
README explicando:
  - Como rodar o projeto.
  - Suas escolhas de design.
  - O que faria diferente com mais tempo.

---

## Premissas e Stack

Stack Atual
Java 8
Angular ou React
JAX-RS
JDBC
CDI
JPA + Hibernate
JMS
Observação: Essa stack reflete parte do nosso ambiente atual. O que queremos ver é como você aplica boas práticas dentro dessas ferramentas.

Premissas
Código deve ser versionado em repositório Git. Ex: GitHub, GitLab ou Bitbucket.
Deve rodar em WildFly 10.
Compatibilidade com Chrome/Edge.


---

## O que esperamos de você?

Clareza de pensamento.
Decisões técnicas justificadas.
Código limpo e testável.
Comunicação clara via README.
Para nós, este desafio é uma simulação prática do papel de desenvolvedor: resolver um problema simples de forma estruturada e comunicar claramente suas decisões.

---

