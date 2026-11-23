.DEFAULT_GOAL := help
COMPOSE = docker compose --env-file .env -f compose.yaml
TEST_COMPOSE = docker compose --env-file .env.test -f compose.test.yaml
-include .env
export JAVA_HOME

.PHONY: help build build-backend build-frontend build-javafx build-console up start javafx console cli stop down logs ps test test-backend test-frontend test-e2e test-down smoke


build: build-backend build-frontend

build-frontend:
	$(COMPOSE) build frontend

build-backend:
	$(COMPOSE) build backend

build-javafx:
	cd javafx_client && ../backend/mvnw -q -DskipTests package

build-console:
	$(COMPOSE) --profile cli build console

up:
	$(COMPOSE) up --build -d --wait frontend

start: up

javafx: up
	cd javafx_client && API_URL=http://localhost:$(BACKEND_PORT) EXPORT_DIR=../exports ../backend/mvnw -q javafx:run

console: up
	$(COMPOSE) --profile cli run --build --rm console

cli: console

stop:
	$(COMPOSE) stop

down:
	$(COMPOSE) --profile cli --profile smoke down

logs:
	$(COMPOSE) logs -f backend

ps:
	$(COMPOSE) ps

.NOTPARALLEL: test test-backend test-frontend test-e2e

test: test-backend test-frontend test-down

test-backend:
	$(TEST_COMPOSE) run --build --rm backend-tests

test-frontend:
	$(TEST_COMPOSE) run --build --rm frontend-tests

test-e2e: test-down
	$(TEST_COMPOSE) --profile e2e run --build --rm frontend-e2e

test-down:
	$(TEST_COMPOSE) --profile e2e down

smoke:
	$(COMPOSE) run --build --rm frontend-smoke
