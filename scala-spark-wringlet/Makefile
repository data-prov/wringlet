#
# https://github.com/data-prov/wringlet/blob/main/scala-spark-wringlet/Makefile
#
SCALA_MINOR_VERSION ?= $(shell cat SCALA_MINOR_VERSION)
SCALA_PACKAGE_NAME ?= dp-spark
SCALA_PACKAGE_VERSION ?= $(shell cat ../VERSION)
SCALA_PACKAGE_JAR ?= $(SCALA_PACKAGE_NAME)_$(SCALA_MINOR_VERSION)-$(SCALA_PACKAGE_VERSION).jar

.PHONY: info check-version-format-branch add-branch-name-to-version increment-dev-version bump-to-major-version bump-to-minor-version bump-to-patch-version clean compile build test publish-local lint fix-lint format-check format check fix

check-version-format-branch:
	$(MAKE) -C .. check-version-format-branch

add-branch-name-to-version:
	$(MAKE) -C .. add-branch-name-to-version

increment-dev-version:
	$(MAKE) -C .. increment-dev-version

bump-to-major-version:
	$(MAKE) -C .. bump-to-major-version

bump-to-minor-version:
	$(MAKE) -C .. bump-to-minor-version

bump-to-patch-version:
	$(MAKE) -C .. bump-to-patch-version

info:
	@echo "Package version: $(SCALA_PACKAGE_VERSION) - Scala version: $(SCALA_MINOR_VERSION)"

clean:
	rm -rf target project/project project/target

# Build project
compile:
	sbt compile

build:
	sbt package
	ls -lFh target/scala-$(SCALA_MINOR_VERSION)/$(SCALA_PACKAGE_JAR)

test:
	sbt test

publish-local:
	sbt 'set isSnapshot := true' publishLocal publishM2

lint:
	sbt 'scalafix --check'

fix-lint:
	sbt scalafix

format-check:
	sbt scalafmtCheck

format:
	sbt scalafmt

check: lint format-check

fix: fix-lint format
