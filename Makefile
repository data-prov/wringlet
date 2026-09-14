#
# https://github.com/data-prov/wringlet/blob/main/Makefile
#
SC_DIR ?= scala-spark-wringlet
PY_DIR ?= pyspark-wringlet
VERSION_FILE ?= VERSION
VERSION := $(shell cat $(VERSION_FILE))
SCALA_PACKAGE_NAME ?= dp-spark
SCALA_MINOR_VERSION ?= $(shell cat $(SC_DIR)/SCALA_MINOR_VERSION)
SCALA_PACKAGE_JAR ?= $(SCALA_PACKAGE_NAME)_$(SCALA_MINOR_VERSION)-$(VERSION).jar

.PHONY: info check-version-format-branch add-branch-name-to-version increment-dev-version bump-to-major-version bump-to-minor-version bump-to-patch-version scala-check scala-fix scala-build python-clean python-init-uv-python python-bump-package python-bump-to-major-version python-bump-to-minor-version python-bump-to-patch-version python-increment-dev-version python-init python-build python-check python-fix python-test python-publish python-publish-testpypi python-install-local python-run-local

check-version-format-branch:
	@VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+(\.dev[0-9]+)?(\+[a-zA-Z0-9\.]+)?$$"; \
	NORMALIZED_BRANCH=$$(git rev-parse --abbrev-ref HEAD | sed 's/[^a-zA-Z0-9]/./g' | tr '[:upper:]' '[:lower:]'); \
	echo "Checking version format on branch: $$(git rev-parse --abbrev-ref HEAD), version: $(VERSION)"; \
	if [ "$$(git rev-parse --abbrev-ref HEAD)" != "main" ]; then \
		if ! echo "$(VERSION)" | grep -Eq "$$VERSION_REGEX"; then \
			echo "WARNING: Version does not match the required format (X.Y.Z or X.Y.Z.devN or X.Y.Z(.devN)+branch.name)"; \
			echo "See: https://peps.python.org/pep-0440/ for more information."; \
		fi; \
		if ! echo "$(VERSION)" | grep -Eq "\+$$NORMALIZED_BRANCH$$"; then \
			echo "WARNING: Version does not end with the normalized branch name ($$NORMALIZED_BRANCH)"; \
			echo "Recommendation: run 'make add-branch-name-to-version' to correct the version."; \
		else \
			echo "Version format is correct for branch: $$(git rev-parse --abbrev-ref HEAD)."; \
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
	$(eval BRANCH_NAME=$(shell [ -n "$$GITHUB_HEAD_REF" ] && echo "$$GITHUB_HEAD_REF" || echo "$${GITHUB_REF#refs/heads/}" | sed 's|refs/pull/.*||' | tr '[:upper:]' '[:lower:]'))
	$(eval CLEAN_BRANCH_NAME=$(shell echo "$(BRANCH_NAME)" | sed 's/[^a-zA-Z0-9]/./g' | tr '[:upper:]' '[:lower:]'))
	$(eval NEW_VERSION=$(shell echo "$(VERSION)" | sed 's/\+.*//'))
	$(eval NEW_VERSION=$(NEW_VERSION)+$(CLEAN_BRANCH_NAME))
	@echo "Updating version from: $(VERSION) to: $(NEW_VERSION)"
	@echo "$(NEW_VERSION)" > $(VERSION_FILE)
	@git add $(VERSION_FILE)

increment-dev-version:
	@VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+$$"; \
	DEV_VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+\.dev[0-9]+$$"; \
	echo "Current version: $(VERSION)"; \
	if echo "$(VERSION)" | grep -Eq "$$VERSION_REGEX"; then \
		NEW_VERSION=$$(echo "$(VERSION)" | awk -F. '{print $$1"."$$2"."$$3+1".dev0"}'); \
		echo "Updating release version to the next minor dev version: $$NEW_VERSION"; \
	elif echo "$(VERSION)" | grep -Eq "$$DEV_VERSION_REGEX"; then \
		NEW_VERSION=$$(echo "$(VERSION)" | awk -F'.dev' '{print $$1".dev"($$2+1)}'); \
		echo "Updating dev version to: $$NEW_VERSION"; \
	else \
		echo "ERROR: Version format is invalid. Should be X.Y.Z or X.Y.Z.devN"; \
		exit 1; \
	fi; \
	echo "$$NEW_VERSION" > $(VERSION_FILE); \
	git add $(VERSION_FILE)

bump-to-minor-version:
	@VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+\.dev[0-9]+$$"; \
	echo "Current version: $(VERSION)"; \
	if echo "$(VERSION)" | grep -Eq "$$VERSION_REGEX"; then \
		NEW_VERSION=$$(echo "$(VERSION)" | sed 's/\.dev[0-9]*//'); \
		echo "Bumping to the minor version: $$NEW_VERSION"; \
	else \
		echo "ERROR: Version format is invalid. Should be X.Y.Z.devN"; \
		exit 1; \
	fi; \
	echo "$$NEW_VERSION" > $(VERSION_FILE); \
	git add $(VERSION_FILE)

bump-to-major-version:
	@VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+(\.dev[0-9]+)?(\+[a-zA-Z0-9._-]+)?$$"; \
	echo "Current version: $(VERSION)"; \
	if echo "$(VERSION)" | grep -Eq "$$VERSION_REGEX"; then \
		NEW_VERSION=$$(echo "$(VERSION)" | sed 's/\(\.dev[0-9]*\)\?\(\+[a-zA-Z0-9._-]*\)\?//g' | awk -F'[.]' '{print $$1+1".0.0"}'); \
		echo "Bumping to the major version: $$NEW_VERSION"; \
	else \
		echo "ERROR: Version format is invalid. Should be X.Y.Z, X.Y.Z.devN, X.Y.Z+branch.name, or X.Y.Z.devN+branch.name"; \
		exit 1; \
	fi; \
	echo "$$NEW_VERSION" > $(VERSION_FILE); \
	git add $(VERSION_FILE)

bump-to-patch-version:
	@VERSION_REGEX="^[0-9]+\.[0-9]+\.[0-9]+(\.dev[0-9]+)?(\+[a-zA-Z0-9._-]+)?$$"; \
	echo "Current version: $(VERSION)"; \
	if echo "$(VERSION)" | grep -Eq "$$VERSION_REGEX"; then \
		NEW_VERSION=$$(echo "$(VERSION)" | sed 's/\(\.dev[0-9]*\)\?\(\+[a-zA-Z0-9._-]*\)\?//g' | awk -F'[.]' '{print $$1"."$$2"."$$3+1}'); \
		echo "Bumping to the patch version: $$NEW_VERSION"; \
	else \
		echo "ERROR: Version format is invalid. Should be X.Y.Z, X.Y.Z.devN, X.Y.Z+branch.name, or X.Y.Z.devN+branch.name"; \
		exit 1; \
	fi; \
	echo "$$NEW_VERSION" > $(VERSION_FILE); \
	git add $(VERSION_FILE)

info:
	@echo "Package version: $(VERSION) - Scala version: $(SCALA_MINOR_VERSION)"

# Build project
scala-check:
	@echo "Running Scala lint and format checks..."
	$(MAKE) -C $(SC_DIR) check
	@echo "Scala lint and format checks complete."

scala-fix:
	@echo "Running Scala lint fixes and formatting code..."
	$(MAKE) -C $(SC_DIR) fix
	@echo "Scala lint fixes and formatting complete."

scala-build:
	@echo "Testing and packaging the Scala project..."
	$(MAKE) -C $(SC_DIR) compile test build publish-local
	@echo "Scala build complete."
	@echo "Copying $(SCALA_PACKAGE_JAR) JAR to PySpark directory..."
	cp -f $(SC_DIR)/target/scala-$(SCALA_MINOR_VERSION)/$(SCALA_PACKAGE_JAR) $(PY_DIR)/src/wringlet/jars/
	@echo "$(SCALA_PACKAGE_JAR) JAR copy complete."

python-clean:
	@echo "Cleaning Python project..."
	$(MAKE) -C $(PY_DIR) clean
	@echo "Python project cleaned."

python-init-uv-python:
	@echo "Initializing Python installation with uv..."
	$(MAKE) -C $(PY_DIR) init-uv-python

python-bump-package:
	@echo "Bumping the Python package version..."
	$(MAKE) -C $(PY_DIR) bump-package

python-bump-to-major-version:
	@echo "[Python] Bumping to the major version and then into related files..."
	$(MAKE) -C $(PY_DIR) bump-to-major-version
	$(MAKE) -C $(PY_DIR) bump-package

python-bump-to-minor-version:
	@echo "[Python] Bumping to the minor version and then into related files..."
	$(MAKE) -C $(PY_DIR) bump-to-minor-version
	$(MAKE) -C $(PY_DIR) bump-package

python-bump-to-patch-version:
	@echo "[Python] Bumping to the patch version and then into related files..."
	$(MAKE) -C $(PY_DIR) bump-to-patch-version
	$(MAKE) -C $(PY_DIR) bump-package

python-increment-dev-version:
	@echo "[Python] Incrementing the package version and bumping it into related files..."
	$(MAKE) -C $(PY_DIR) increment-dev-version
	$(MAKE) -C $(PY_DIR) bump-package

python-init:
	@echo "Initializing Python project..."
	$(MAKE) -C $(PY_DIR) init update

python-build: python-init
	@echo "Building Python project..."
	$(MAKE) -C $(PY_DIR) build
	@echo "Python build complete."

python-check:
	@echo "Running Python lint, format and type checks..."
	$(MAKE) -C $(PY_DIR) check 
	@echo "Python lint, format and type checks complete."
	
python-fix:
	@echo "Running Python linter fixes and formatting code..."
	$(MAKE) -C $(PY_DIR) fix

python-test:
	@echo "Running Python tests..."
	$(MAKE) -C $(PY_DIR) test

python-publish: python-build
	@echo "Publishing Python package to PyPI..."
	$(MAKE) -C $(PY_DIR) publish

python-publish-testpypi:
	@echo "Publishing Python package to TestPyPI..."
	$(MAKE) -C $(PY_DIR) publish-testpypi

python-install-local:
	@echo "Installing Python package locally..."
	$(MAKE) -C $(PY_DIR) install-local

python-run-local:
	@echo "Run Python job locally..."
	$(MAKE) -C $(PY_DIR) run-local
