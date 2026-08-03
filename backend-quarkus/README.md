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

### Ingest generated dataset metadata folders at startup

You can mount generated metadata folders (for example `.../gen/demo`) into the container and let the backend ingest
them during boot:

```properties
conquery.metadata.enabled=true
conquery.metadata.root-path=/data/gen
conquery.metadata.folders[0]=demo
```

This reads `conceptTrees/*.concept.json` and `tables/*.table.json` from each configured folder and loads them as
dataset metadata.

### Concept JSON migration helpers

Some Dropwizard concept metadata still uses connector column references in the old `table.column` form. The migration
helper `scripts/normalize_concept_connector_columns.py` rewrites those references for the Quarkus metadata loader:

- connector `"column": "table.column"` becomes `"table": "table", "column": "column"`
- filter `"column": "table.column"` becomes `"column": "column"`
- validity-date `column`, `startColumn`, and `endColumn` references become local column names

Run it from the repository root:

```bash
python3 scripts/normalize_concept_connector_columns.py path/to/*.concept.json
```

Use `--dry-run` first to see how many connector, filter, connector-select, and validity-date columns would be changed. Select fields
covered by the script are `column`, `startColumn`, `endColumn`, `subtractColumn`, `distinctByColumn`, `distinctBy`, and
the column values in `flags`. This script is intended to grow with additional concept metadata migration steps as they
become necessary.

### Polymorphic metadata models

Metadata models are code-first. Java model classes, Jackson annotations, Bean Validation constraints, and OpenAPI
`@Schema` annotations are used both for loading metadata files and for generating their schemas. Registered model
families are exposed in the OpenAPI document at `GET /q/openapi` as `oneOf` schemas with discriminator mappings.

Filter, select, and concept-condition implementations are extensible from another CDI-enabled JAR. A filter extension provides:

1. A concrete class implementing `FilterDefinition`, annotated with OpenAPI `@Schema`.
2. A CDI bean implementing `FilterDefinitionProvider<T>` for its type id, model class, and business-model conversion.

Connector selects use the equivalent `SelectDefinition` and `SelectDefinitionProvider<T>` extension points. Unknown
types fail startup by default; `conquery.metadata.strict-filter-types` and
`conquery.metadata.strict-select-types` can independently switch their family to warning-and-skip behavior.

Concept-tree conditions use `ConceptCondition` and `ConceptConditionProvider<T>`. Because skipping a condition would
change the meaning of a concept node, unknown condition types always fail metadata validation. The built-in condition
types are `EQUAL`, `PREFIX_LIST`, `PREFIX_RANGE`, `COLUMN_EQUAL`, `PRESENT`, `AND`, `OR`, and `NOT`. The old `GROOVY`
condition is intentionally not registered because script execution requires a separate security and runtime decision.

The generic `PolymorphicModelRegistry` validates duplicate type ids and base/model compatibility at startup. Its
registrations are also installed into Jackson and projected into OpenAPI. Extension JARs must be discoverable by
Quarkus bean and Jandex indexing so their provider and annotated model class are available during application assembly.

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
