# Makefile for PartyTime Server
# Works with both Docker and Podman

# Detect if using podman or docker
CONTAINER_ENGINE ?= $(shell command -v podman 2> /dev/null && echo podman || echo docker)
COMPOSE_CMD = $(CONTAINER_ENGINE)-compose

# Image name and tag
IMAGE_NAME = partytime-server
IMAGE_TAG ?= latest
FULL_IMAGE = $(IMAGE_NAME):$(IMAGE_TAG)

.PHONY: help
help: ## Show this help message
	@echo "PartyTime Server - Container Build Commands"
	@echo "Using container engine: $(CONTAINER_ENGINE)"
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

.PHONY: build
build: ## Build the application container image
	$(CONTAINER_ENGINE) build -t $(FULL_IMAGE) .

.PHONY: build-no-cache
build-no-cache: ## Build the application without using cache
	$(CONTAINER_ENGINE) build --no-cache -t $(FULL_IMAGE) .

.PHONY: up
up: ## Start all services (app + postgres)
	$(COMPOSE_CMD) up -d

.PHONY: down
down: ## Stop all services
	$(COMPOSE_CMD) down

.PHONY: restart
restart: down up ## Restart all services

.PHONY: logs
logs: ## Show logs from all services
	$(COMPOSE_CMD) logs -f

.PHONY: logs-app
logs-app: ## Show logs from application only
	$(COMPOSE_CMD) logs -f app

.PHONY: logs-db
logs-db: ## Show logs from database only
	$(COMPOSE_CMD) logs -f postgres

.PHONY: ps
ps: ## Show running containers
	$(COMPOSE_CMD) ps

.PHONY: clean
clean: ## Remove containers and volumes
	$(COMPOSE_CMD) down -v

.PHONY: shell-app
shell-app: ## Open a shell in the application container
	$(CONTAINER_ENGINE) exec -it partytime-app sh

.PHONY: shell-db
shell-db: ## Open a PostgreSQL shell
	$(CONTAINER_ENGINE) exec -it partytime-postgres psql -U PartyTime -d PartyTime

.PHONY: rebuild
rebuild: build down up ## Rebuild and restart all services

.PHONY: package
package: build ## Build and save the image as a tar file
	$(CONTAINER_ENGINE) save $(FULL_IMAGE) -o $(IMAGE_NAME)-$(IMAGE_TAG).tar
	@echo "Image saved to $(IMAGE_NAME)-$(IMAGE_TAG).tar"

.PHONY: load
load: ## Load the image from a tar file
	$(CONTAINER_ENGINE) load -i $(IMAGE_NAME)-$(IMAGE_TAG).tar

.DEFAULT_GOAL := help
