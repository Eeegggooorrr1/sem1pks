.DEFAULT_GOAL := help
COMPOSE = docker compose --env-file .env -f compose.yaml
TEST_COMPOSE = docker compose --env-file .env.test -f compose.test.yaml

.PHONY: help build up start cli stop down logs ps test test-backend test-frontend test-down smoke

build:
	$(COMPOSE) --profile cli build backend frontend

up:
	$(COMPOSE) up --build -d --wait backend

start: up
	$(COMPOSE) run --build --rm frontend

cli:
	$(COMPOSE) run --build --rm frontend

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
