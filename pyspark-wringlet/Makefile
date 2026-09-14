#
# https://github.com/data-prov/wringlet/blob/main/pyspark-wringlet/Makefile
#

PYTHON_VERSION ?= 3.12
PYTHON ?= .venv/bin/python
UV ?= uv
PYTHON_PACKAGE_NAME ?= pyspark-data-provenance
VERSION_FILE ?= ../VERSION
VERSION := $(shell cat $(VERSION_FILE))
SCALA_MINOR_VERSION ?= $(shell cat ../scala-spark-wringlet/SCALA_MINOR_VERSION)
SCALA_ORG ?= org.dataprov.dp
SCALA_PACKAGE_NAME ?= dp-spark
SCALA_PACKAGE_JAR ?= $(SCALA_ORG):$(SCALA_PACKAGE_NAME)_$(SCALA_MINOR_VERSION):$(VERSION)

PYPI_PROJECT_URL ?= https://pypi.org/project/$(PYTHON_PACKAGE_NAME)/
UV_PUBLISH_DOC_URL ?= https://docs.astral.sh/uv/guides/package/\#publishing-your-package

.PHONY: help init-uv-python init update check-version-format-branch add-branch-name-to-version increment-dev-version bump-to-major-version bump-to-minor-version bump-to-patch-version build install-local run-local test lint fix format type-check check clean require-uv-publish-token publish publish-testpypi

help: ## Show this help message
	@echo "Usage: make [target]"
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-15s %s\n", $$1, $$2}'

init-uv-python: ## Install Python $(PYTHON_VERSION) for uv
	@$(UV) python install $(PYTHON_VERSION)

init: ## Lock and sync dependencies
	@$(UV) lock
	@$(UV) sync --all-groups

update: ## Upgrade and sync dependencies
	@$(UV) lock --upgrade
	@$(UV) sync --all-groups

check-version-format-branch:
   $(eval BRANCH=$(shell git rev-parse --abbrev-ref HEAD))
   @VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+(\.dev[0-9]+)?(\+[a-zA-Z0-9\.]+)?$$"; \
   NORMALIZED_BRANCH=$$(echo $(BRANCH) | sed 's/[^a-zA-Z0-9]/./g' | tr '[:upper:]' '[:lower:]'); \
   echo "Checking version format on branch: $(BRANCH), version: $(VERSION)"; \
   if [ "$(BRANCH)" != "main" ]; then \
      if ! echo "$(VERSION)" | grep -Eq "$$VERSION_REGEX"; then \
         echo "WARNING: Version does not match the required format (X.Y.Z or X.Y.Z.devN or X.Y.Z(.devN)+branch.name)"; \
         echo "See: https://peps.python.org/pep-0440/ for more information."; \
      fi; \
      if ! echo "$(VERSION)" | grep -Eq "\+$$NORMALIZED_BRANCH$$"; then \
         echo "WARNING: Version does not end with the normalized branch name ($$NORMALIZED_BRANCH)"; \
         echo "Recommendation: run 'make add-branch-name-to-version' to correct the version."; \
      else \
         echo "Version format is correct for branch: $(BRANCH)."; \
      fi; \
   else \
      echo "Skipping branch name check on main branch."; \
      if ! echo "$(VERSION)" | grep -Eq "$$VERSION_REGEX"; then \
         echo "WARNING: Version does not match the required format (X.Y.Z or X.Y.Z.devN)"; \
         echo "See: https://peps.python.org/pep-0440/ for more information."; \
      else \
         echo "Version format is correct for main branch."; \
      fi; \
   fi

add-branch-name-to-version:
	$(MAKE) -C .. add-branch-name-to-version

increment-dev-version:
	$(MAKE) -C .. increment-dev-version

bump-to-minor-version:
	$(MAKE) -C .. bump-to-minor-version

bump-to-major-version:
	$(MAKE) -C .. bump-to-major-version

bump-to-patch-version:
	$(MAKE) -C .. bump-to-patch-version

bump-package: ## Bump the package version of the project.
	@sed -i.bak 's/^version = .*/version = "$(VERSION)"/' pyproject.toml && rm pyproject.toml.bak
	@git add pyproject.toml
	@$(UV) lock
	@$(UV) lock --upgrade
	@$(UV) sync --all-groups
	@git add uv.lock

build: ## Build project wheel
	@rm -rf dist
	@$(UV) build --wheel

test: ## Run tests
	@$(UV) run --group dev pytest

lint: ## Run Ruff linter
	@$(UV) run --group dev ruff check .

fix-lint: ## Fix linter issues
	$(UV) run --group dev ruff check --fix .

format: ## Format with Ruff
	@$(UV) run --group dev ruff format .

format-check:
	@$(UV) run --group dev ruff format --check .

type-check: ## Run mypy on source and tests
	@$(UV) run --group dev mypy src

check: lint format-check type-check ## Run lint, format and type checks

fix: fix-lint format ## Fix linter and format issues

clean: ## Remove caches and build artifacts
	@rm -rf .venv dist build .pytest_cache .mypy_cache .ruff_cache
	@find . -type d -name __pycache__ -exec rm -rf {} +
	@find . -type f -name "*.pyc" -delete

require-uv-publish-token: ## Check UV_PUBLISH_TOKEN for production PyPI publish
	@if [ -n "$(UV_PUBLISH_TOKEN)" ]; then \
		echo "UV_PUBLISH_TOKEN is set."; \
		echo "It is expected to be the token for production PyPI publishing."; \
		echo "See: $(UV_PUBLISH_DOC_URL)"; \
	else \
		echo "UV_PUBLISH_TOKEN is not set."; \
		echo "Set UV_PUBLISH_TOKEN to your production PyPI token before publishing."; \
		echo "See: $(UV_PUBLISH_DOC_URL)"; \
		exit 1; \
	fi

publish: require-uv-publish-token ## Publish built artifacts to PyPI
	@$(UV) publish
	@echo "The Python package has been published on $(PYPI_PROJECT_URL)"

publish-from-ci: ## Publish built artifacts to PyPI from GitHub Actions
	@$(UV) publish
	@echo "The Python package has been published on $(PYPI_PROJECT_URL)"

publish-testpypi: ## Publish built artifacts to TestPyPI
	@$(UV) publish --publish-url https://test.pypi.org/legacy/

install-local: ## Install the Python wheel locally
	@$(UV) pip install --python $(PYTHON) -U pyspark
	@$(UV) pip install --python $(PYTHON) --force-reinstall \
		.

run-local: ## Run package demo
	@mkdir -p logs
	@spark-submit --packages $(SCALA_PACKAGE_JAR) \
		src/wringlet/jobs/getting_started_job.py | \
		tee logs/getting_started_job.log

