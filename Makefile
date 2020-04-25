compose=docker-compose -f docker-compose.yml -p sssl-doktor-backend

default: help

up: ## Spin up services
	$(compose) up -d

stop: ## Stop services
	$(compose) stop

down: ## Destroy all services and volumes
	$(compose) down -v

psql: ## Open psql console
	$(compose) exec db bash -c "psql -U sssl-doktor-backend-dev"

help: ## This help message
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' -e 's/:.*#/: #/' | column -t -s '##'