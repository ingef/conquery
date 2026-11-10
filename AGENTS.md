# Agent Guide for Conquery

This file helps automated agents and contributors navigate the repository. More specific instructions in nested `AGENTS.md` files take precedence for files in their subtree.

## Project overview

Conquery is a web application for composing and executing queries against large, event-like datasets. The repository combines a Java 21 backend with a React/TypeScript frontend.

- Backend build: Maven multi-module project rooted at `pom.xml`.
- Frontend build: Vite with Node.js 24 and pnpm 11.
- End-to-end tests: Cypress, managed from the repository root.
- Deployment: Dockerfiles for backend and frontend, plus a Helm chart.

Before changing backend code, read `backend/AGENTS.md`; its Java, toolchain, testing, and commit guidance applies within that subtree.

## Repository layout (high level)

| Path | Purpose |
| --- | --- |
| `backend/` | Main Java service: APIs, domain model, query execution, persistence, configuration, and the server-rendered admin UI |
| `executable/` | Java entry point and Maven configuration for the shaded, runnable Conquery JAR |
| `news-service/` | Java module that provides file-system-backed news items |
| `autodoc/` | Java tool that generates Markdown documentation from backend types and Javadocs |
| `frontend/` | React/TypeScript web client, development mock API, Storybook, and frontend container files |
| `cypress/` | End-to-end tests for the React frontend and backend admin UI |
| `charts/conquery/` | Helm chart, Kubernetes templates, and default deployment values |
| `docs/` | Hand-written user and operator documentation plus supporting resources |
| `tutorial/mimic_iii_demo/` | Jupyter notebooks, JSON schemas, and Python helpers for the MIMIC-III import tutorial |
| `migrations/` | Example Groovy scripts for migrations of persisted backend data |
| `scripts/` | Shell scripts for builds, local end-to-end environments, and loading test data |
| `images/` | Images and logos used by the root README |
| `.github/` | CI, release, security scanning, ownership, and dependency-management configuration |

## Reference docs

| Topic | Location |
| --- | --- |
| Project setup, demo, tests, and data integration | `README.md` |
| Backend-specific agent guidance | `backend/AGENTS.md` |
| Frontend setup and architecture notes | `frontend/README.md` |
| Cypress organization and conventions | `cypress/README.md` |
| Authentication configuration | `docs/authentication.md` |
| Custom forms | `docs/custom_forms.md` |
| Helm deployment | `charts/conquery/README.md` |
| MIMIC-III import tutorial | `tutorial/mimic_iii_demo/README.md` |
