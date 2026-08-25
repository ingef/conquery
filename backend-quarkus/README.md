# Conquery Quarkus Backend

This module is the migration target for replacing the Dropwizard backend with Quarkus in small, safe increments.

## Current state

- Maven module: `backend-quarkus`
- Quarkus REST runtime with JSON support
- Health and OpenAPI extensions
- Dataset, concept, filter, form-config, query, frontend-config, and user API slices
- In-memory and Xodus-backed manager metadata storage
- Read-only folder-based ingestion of dataset definitions into a rich in-memory model
- Extensible polymorphic metadata and query models

The Dropwizard `backend` remains the production backend. Entity-query execution and some compatibility slices are not
implemented in Quarkus yet.

## Run locally

From repository root:

```bash
./mvnw -pl backend-quarkus quarkus:dev
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
dataset metadata. Dataset definitions are derived state: they are rebuilt on every application start and are not
persisted in Xodus. Change the source metadata and restart the application to update them.

Each `dataset.json` can select a Quarkus named datasource. If omitted, `dataSource` defaults to the dataset id:

```json
{
  "id": "claims",
  "label": "Claims",
  "dataSource": "analytics"
}
```

The loaded repository retains the complete typed filter, select, and condition definitions. REST resources map this
internal model to their dedicated response records, while query planning can use the same repository without loading
or persisting a second dataset representation.

### Configure SQL datasources

SQL execution is disabled until `conquery.sql.enabled=true`. Dataset datasource names refer to standard Quarkus named
JDBC datasources. HANA and ClickHouse use custom JDBC drivers and therefore use `db-kind=other`:

```properties
conquery.sql.enabled=true
conquery.sql.datasources.analytics.dialect=CLICKHOUSE

quarkus.datasource.analytics.db-kind=other
quarkus.datasource.analytics.jdbc.driver=com.clickhouse.jdbc.ClickHouseDriver
quarkus.datasource.analytics.jdbc.url=${ANALYTICS_JDBC_URL}
quarkus.datasource.analytics.jdbc.transactions=disabled
quarkus.datasource.analytics.username=${ANALYTICS_USER}
quarkus.datasource.analytics.password=${ANALYTICS_PASSWORD}
```

For HANA, set `conquery.sql.datasources.<name>.dialect=HANA` and use `com.sap.db.jdbc.Driver`. Quarkus manages the
Agroal pools and contributes their readiness checks through SmallRye Health. Datasource names and `db-kind` are
build-time configuration; URLs, credentials, activation, and pool sizing can remain runtime configuration. When SQL
is enabled, startup fails if a loaded dataset references a datasource without Conquery dialect metadata or without a
matching Quarkus named datasource.

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

## Polymorphic model plugins

Metadata models are code-first. Java model classes, Jackson annotations, Bean Validation constraints, and OpenAPI
`@Schema` annotations are used both for loading metadata files and for generating their schemas. Registered model
families are exposed in the OpenAPI document at `GET /q/openapi` as `oneOf` schemas with discriminator mappings.

These plugins are ordinary JAR libraries containing CDI beans. They are not full Quarkus extensions and they are not
hot-loaded. The backend is packaged as a Quarkus mutable JAR with an official user-provider directory named
`providers`. A plugin becomes part of the application only when Quarkus reaugmentation runs after the JAR was placed
in that directory. This is the same mechanism for a local unpacked distribution and for a container image. See the
Quarkus [reaugmentation guide](https://quarkus.io/guides/reaugmentation) for the underlying packaging mechanism.

This mechanism is separate from the Dropwizard `plugins` array in `config.json`. Entries such as `FORM_BACKEND` configure
runtime integrations; they do not install polymorphic model implementations.

### Supported extension points

| Model family | Model base | Provider bean | Contract |
| --- | --- | --- | --- |
| Metadata filters | `FilterDefinition` | `FilterDefinitionProvider<T>` | Stable plugin API |
| Query filter values | `FilterValue` | `FilterValueProvider<T>` | Provisional backend API |
| Connector selects | `SelectDefinition` | `SelectDefinitionProvider<T>` | Provisional backend API |
| Concept-level selects | `ConceptSelectDefinition` | `ConceptSelectDefinitionProvider<T>` | Provisional backend API |
| Concept-tree conditions | `ConceptCondition` | `ConceptConditionProvider<T>` | Provisional backend API |

A metadata filter plugin provides:

1. A concrete class implementing `FilterDefinition`, annotated with OpenAPI `@Schema` and
   `@PolymorphicModelSubtype(base = FilterDefinition.class, id = "...")`.
2. A CDI bean implementing `FilterDefinitionProvider<T>` for its model class, conversion, and integrity validation.
3. The filter value model types accepted by that provider. Each model type needs a registered `FilterValueProvider<T>`.

Connector selects use the equivalent `SelectDefinition` and `SelectDefinitionProvider<T>` extension points. Concept-level
selects form a separate family with `ConceptSelectDefinition` and `ConceptSelectDefinitionProvider<T>`. Unknown types
fail startup by default; `conquery.metadata.strict-filter-types` and
`conquery.metadata.strict-select-types` can independently switch their family to warning-and-skip behavior.

Concept-tree conditions use `ConceptCondition` and `ConceptConditionProvider<T>`. Because skipping a condition would
change the meaning of a concept node, unknown condition types always fail metadata validation. The built-in condition
types are `EQUAL`, `PREFIX_LIST`, `PREFIX_RANGE`, `COLUMN_EQUAL`, `PRESENT`, `AND`, `OR`, and `NOT`. The old `GROOVY`
condition is intentionally not registered because script execution requires a separate security and runtime decision.

Query filter values use the separate `FilterValue` and `FilterValueProvider<T>` family. Their discriminator ids may
overlap metadata filter definitions (for example `SELECT`) because polymorphic registrations are scoped by base type.
Filter providers declare their accepted `FilterValue` model classes through `acceptedValueTypes()`. Startup verifies
that each declared model has a registered value provider, and metadata conversion verifies the emitted value type.
Entity-query execution is intentionally not implemented yet. The filter value and entity-history API models are kept,
but the entity-history and entity-resolution endpoints return `501 Not Implemented` until their business semantics are migrated.

The generic `PolymorphicModelRegistry` validates duplicate type ids and base/model compatibility at startup. Its
registrations are also installed into Jackson and projected into OpenAPI. Extension JARs must be discoverable by
Quarkus bean and Jandex indexing so their provider and annotated model class are available during application assembly.

### Develop a plugin JAR

Use a separate Maven project with Java 21. Filter plugins compile against the dedicated plugin API, not the backend
application. Keep the dependency `provided`, because the host distribution supplies the API classes:

```xml
<dependency>
    <groupId>com.bakdata.conquery</groupId>
    <artifactId>backend-quarkus-plugin-api</artifactId>
    <version>${conquery.version}</version>
    <scope>provided</scope>
</dependency>
```

The API artifact must be available from a Maven repository. For local development from this repository, install it
first:

```bash
./mvnw -pl backend-quarkus-plugin-api -am install -DskipTests
```

Quarkus discovers the plugin's CDI beans and OpenAPI models from its build-time index. Generate
`META-INF/jandex.idx` in the plugin JAR:

```xml
<plugin>
    <groupId>io.smallrye</groupId>
    <artifactId>jandex-maven-plugin</artifactId>
    <version>3.3.1</version>
    <executions>
        <execution>
            <id>make-index</id>
            <goals>
                <goal>jandex</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

For each implementation:

1. Choose a discriminator unique within its model family. IDs may be reused across different families.
2. Implement a concrete model with public Jackson-accessible properties.
3. Add `@Schema(name = "...")` and `@PolymorphicModelSubtype(base = ..., id = "...")` to the model.
4. Implement the matching provider interface and annotate the provider with a CDI scope such as `@ApplicationScoped`.
5. Return the accepted frontend value discriminator IDs from `acceptedValueTypes()`, for example `Set.of("STRING")`.
6. Convert the model into a plugin API `FilterResult`. Use `FilterConversionContext.requireColumn()` to validate and
   inspect referenced columns through the shared `ColumnDescriptor` and `ColumnType` dataset contracts. Do not
   construct backend IDs or repository records in a plugin.
7. Run `mvn verify` and confirm the built JAR contains `META-INF/jandex.idx`.

The filter contract is contained in `backend-quarkus-plugin-api` under `com.bakdata.conquery.quarkus.plugin.api`. It
contains the definition bases, provider interface, conversion context/result, shared dataset column descriptors, and
plugin polymorphism annotations. Backend IDs, storage repositories, and implementation DTOs are intentionally
excluded. Query filter-value, select, and condition extension contracts are still provisional and remain in
`backend-quarkus`; extract them into this API before treating them as compatibility-stable third-party extension
points.

The executable example is in `backend-quarkus-plugin-test/plugin`. Its `PLUGIN_PREFIX` filter deliberately lives in a
separate JAR and has no dependency on the backend application artifact.

### Install a plugin in a distribution

First build the mutable application distribution:

```bash
./mvnw -pl backend-quarkus -am package
```

Copy the plugin and any of its dependencies that are not already supplied by Conquery into the generated provider
directory:

```bash
cp my-conquery-plugin.jar backend-quarkus/target/quarkus-app/providers/
```

On PowerShell, use:

```powershell
Copy-Item my-conquery-plugin.jar backend-quarkus/target/quarkus-app/providers/
```

Reaugment the distribution after every plugin addition, replacement, or removal:

```bash
java -Dquarkus.launch.rebuild=true -jar backend-quarkus/target/quarkus-app/quarkus-run.jar
```

Reaugmentation performs the Quarkus build-time discovery and then exits; it does not start the server. Start the
result normally:

```bash
java -jar backend-quarkus/target/quarkus-app/quarkus-run.jar
```

Do not add or change provider JARs after the final reaugmentation. Such changes are not part of CDI, Jackson, or the
generated OpenAPI document. Treat the reaugmented `quarkus-app` directory as one immutable deployment artifact.

For container packaging, use the same sequence while building the image: copy the base `quarkus-app` distribution,
copy plugin JARs into `quarkus-app/providers/`, run the reaugmentation command in a build layer, and copy the resulting
directory into the final runtime image. The running container only needs the normal start command; it must not install
or reaugment plugins at startup. Mutable JAR reaugmentation is a JVM packaging feature and does not apply to native
executables.

For development with `quarkus:dev`, add the plugin as a Maven dependency of `backend-quarkus`; the provider-directory
workflow exercises the packaged distribution.

### Verify the plugin contract

The repository contains a downstream test application because testing the plugin inside `backend-quarkus` would create
a Maven dependency cycle. The reactor order is:

```text
backend-quarkus-plugin-api -> backend-quarkus -> backend-quarkus-test-plugin -> backend-quarkus-plugin-test-application
```

Run the fast application-level contract from the repository root:

```bash
./mvnw -pl backend-quarkus-plugin-test/application -am test
```

`PluginIntegrationTest` verifies all externally observable parts of the contract:

- CDI discovers the provider from the indexed plugin dependency.
- Jackson deserializes the plugin discriminator into the plugin model.
- The real filter assembler invokes the plugin provider and validates its accepted filter-value type.
- `/q/openapi` contains the plugin schema and discriminator mapping.

Run the packaged-distribution contract with:

```bash
./mvnw -pl backend-quarkus-plugin-test/application -am verify
```

`PluginProviderDirectoryIT` packages the application without the plugin on its runtime dependency graph, copies the
real test-plugin JAR into `quarkus-app/providers/`, invokes Quarkus reaugmentation, starts the packaged application in
the production profile, and checks the plugin discriminator and schema through `/q/openapi`. This test covers the same
installation boundary used by a customized container image or a local distribution.

When adding another extension point or changing discovery, update the sample plugin and this integration test. A unit
test that manually constructs a provider is insufficient because it does not exercise Quarkus dependency indexing or
CDI discovery. The fast test gives focused failures; the packaged integration test is the authoritative proof of the
installation mechanism.

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

## Next migration slices

- Introduce shared core module for code that should be reused by both runtimes.
- Add compatibility tests that hit both old and new endpoints for parity.
- Implement entity-query execution and ID resolution against dataset contents.
- Complete concept search, statistics, and remaining frontend metadata compatibility.
