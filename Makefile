.DEFAULT_GOAL := help
COMPOSE = docker compose --env-file .env -f compose.yaml
TEST_COMPOSE = docker compose --env-file .env.test -f compose.test.yaml
-include .env
export JAVA_HOME

.PHONY: help build build-backend build-javafx build-console up start javafx console cli stop down logs ps test test-backend test-frontend test-down smoke

help:
	@echo "make start         - backend + Javafx"

build: build-backend build-javafx

build-backend:
	$(COMPOSE) build backend

build-javafx:
	cd javafx_client && ../backend/mvnw -q -DskipTests package

build-console:
	$(COMPOSE) --profile cli build frontend

up:
	$(COMPOSE) up --build -d --wait backend

start: javafx

javafx: up
	cd javafx_client && API_URL=http://localhost:$(BACKEND_PORT) EXPORT_DIR=../exports ../backend/mvnw -q javafx:run

console: up
	$(COMPOSE) run --build --rm frontend

cli: console

stop:
	$(COMPOSE) stop

down:
	$(COMPOSE) --profile cli --profile smoke down

logs:
	$(COMPOSE) logs -f backend

ps:
	$(COMPOSE) ps

.NOTPARALLEL: test test-backend test-frontend

test: test-backend test-frontend test-down

test-backend:
	$(TEST_COMPOSE) run --build --rm backend-tests

test-frontend:
	$(TEST_COMPOSE) run --build --rm frontend-tests

test-down:
	$(TEST_COMPOSE) down

smoke:
	$(COMPOSE) run --build --rm frontend-smoke