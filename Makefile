.PHONY: help test build docker-up docker-down smoke it ci

BASE_URL ?= http://localhost:8080/url-shortener

help:
	@echo "test        roda os testes unitários (surefire)"
	@echo "build       gera o WAR com o frontend embutido"
	@echo "docker-up   constrói a imagem e sobe o WildFly na porta 8080"
	@echo "docker-down derruba o container"
	@echo "smoke       valida o deploy com curl (criar URL + redirecionar)"
	@echo "it          roda os testes de integração REST contra o container"
	@echo "ci          pipeline completo: test -> build -> docker-up -> smoke -> it -> docker-down"

test:
	mvn -q test

build:
	mvn -q package

docker-up:
	docker compose up -d --build

docker-down:
	docker compose down

smoke:
	bash scripts/smoke.sh $(BASE_URL)

it:
	@docker compose ps -q >/dev/null 2>&1 || docker compose up -d
	mvn -q failsafe:integration-test failsafe:verify -Dit.baseUrl=$(BASE_URL)

ci:
	make test
	make build
	make docker-up
	make smoke
	make it
	make docker-down