COMPOSE := docker compose -f infra/docker/compose.yaml --env-file infra/docker/.env

.PHONY: help install build test lint typecheck up down logs

help:
	@echo "Targets: install build test lint typecheck up down logs"

install:
	npm ci

build: install
	mvn -B -ntp package -DskipTests
	npm run build

test: install
	mvn -B -ntp verify
	npm test

lint: install
	npm run lint

typecheck: install
	npm run typecheck

up:
	$(COMPOSE) up -d --wait

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f
