JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21 2>/dev/null)
export JAVA_HOME

MVN := ./mvnw -B

.PHONY: help setup check-java hooks build test verify run clean check-pr

help:
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-12s %s\n", $$1, $$2}'

setup: check-java hooks ## pierwszy setup: JDK 21, husky, gh login, test build
	@gh auth status >/dev/null 2>&1 || echo "⚠ zaloguj gh: gh auth login (potrzebne do tasków i prompt-history)"
	@chmod +x mvnw scripts/git/*.sh .husky/*
	@$(MVN) -q -DskipTests package && echo "✔ setup OK"

check-java: ## wymaga JDK 21
	@java_version=$$("$${JAVA_HOME:+$$JAVA_HOME/bin/}java" -version 2>&1 | awk -F'"' '/version/ {print $$2}' | cut -d. -f1); \
	[ "$$java_version" = "21" ] || { echo "✖ wymagany JDK 21 (jest: $$java_version). macOS: brew install --cask temurin@21"; exit 1; }

hooks: ## instaluje husky (git hooks)
	@npm install --silent
	@echo "✔ hooki husky aktywne"

build: check-java ## kompilacja + paczka bez testów
	$(MVN) -DskipTests package

test: check-java ## testy
	$(MVN) test

verify: check-java ## build + wszystkie testy (regresja), to samo co pre-push i CI
	$(MVN) verify

run: check-java ## uruchomienie aplikacji lokalnie
	$(MVN) spring-boot:run

clean: ## czyszczenie target/
	$(MVN) clean

check-pr: ## walidacja tytułu/brancha bieżącego PR
	@scripts/git/check-pr.sh "$$(gh pr view --json title -q .title)" "$$(git rev-parse --abbrev-ref HEAD)" "$$(gh pr view --json body -q .body)" && echo "✔ PR OK"
