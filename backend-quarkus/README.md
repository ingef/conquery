# Conquery Quarkus Backend (Migration Scaffold)

This module is the starting point for migrating the Dropwizard backend to Quarkus in small, safe increments.

## Current state

- Maven module: `backend-quarkus`
- Quarkus REST runtime with JSON support
- Health and OpenAPI extensions wired
- Baseline endpoint: `GET /api/ping`

## Run locally

From repository root:

```bash
mvn -pl backend-quarkus quarkus:dev
```

Default port is `8090`.

## Migration strategy

1. Keep `backend` (Dropwizard) running while we port feature slices.
2. Move framework-neutral logic first (services, models, utility layers).
3. Port HTTP resources incrementally:
   - `resources/api/*`
   - `resources/admin/rest/*`
4. Replace Dropwizard-specific primitives with Quarkus equivalents:
   - `@Auth` + Dropwizard Auth -> CDI + Quarkus Security/JAX-RS filters
   - Dropwizard `Task` -> scheduled jobs/admin endpoints
   - `Managed` lifecycle hooks -> Quarkus startup/shutdown events
   - Freemarker views -> keep server-side templates via Quarkus Qute/Freemarker extension or move to frontend
5. Port CLI commands (`ShardCommand`, `PreprocessorCommand`, etc.) to Quarkus CLI/Picocli entrypoints.

## Immediate next slices

- Add one real API resource from `backend/src/main/java/com/bakdata/conquery/resources/api`.
- Introduce shared core module for code that should be reused by both runtimes.
- Add compatibility tests that hit both old and new endpoints for parity.
