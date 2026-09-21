# Complete the SQL connector extraction

## Purpose and constraints

Finish extracting the existing SQL connector so the productive Dropwizard backend uses a framework-neutral module. The extraction must be transferable onto a separate branch based on `develop`, without Quarkus sources, configuration, plugins, or build prerequisites. Quarkus integration is subsequent work.

Preserve existing behavior, including quirks. Preserve recognizable class names, method structure, SQL expressions, and algorithm organization wherever the dependency boundary permits. Do not combine extraction with bug fixes, new validation rules, dependency upgrades, or general cleanup.

## Current state

The branch already contains `dataset-model`, resolved SQL input types, compiler contracts, query-step infrastructure, CTE composition, and condition/filter converters. The backend consumes extracted infrastructure, but `SqlCompiler` is only an interface: backend query conversion, aggregators, selects, forms, dialect implementations, and execution still contain substantive SQL responsibilities.

Uncommitted aggregation conversion work is present and must be reviewed as part of the extraction. In particular, its distinct-sum path applies subtraction where the existing implementation ignores it. It also explicitly rejects duration `distinctBy`; compatibility must be checked against the existing path before accepting that rejection. Neither a cleaner implementation nor an existing new test establishes compatibility.

## Approach

Continue the existing extraction through small, independently reviewable moves. Prefer moving existing SQL algorithms and adapting their inputs over reimplementing them against the new model. Where input adaptation requires a split, leave model resolution in the backend and move SQL construction into the connector while retaining recognizable names and helper structure.

Moving backend domain models wholesale would enlarge the shared dependency surface. Rewriting the compiler would increase behavioral and review risk. Neither is appropriate for this extraction.

## Module boundary

`dataset-model` contains only shared dataset vocabulary needed by the connector. It must not depend on either backend. Additional domain types are not moved merely to make compilation convenient.

`sql-connector` owns backend-independent SQL construction, compilation orchestration, SQL dialect functions, and reusable SQL execution/reading mechanics. It may depend on `dataset-model`, jOOQ, and narrowly needed libraries, but not backend classes, Dropwizard lifecycle/configuration, Quarkus, or repository-based identifier resolution.

The Dropwizard backend owns request deserialization, identifier and saved-query resolution, dataset access, permissions, application configuration, datasource lifecycle, execution tracking, and conversion to application-specific result/error types. Adapters must pass resolved data into the connector and translate results without duplicating SQL algorithms.

Existing JDBC behavior, resource ownership, streaming, error translation, and result column ordering must be preserved when execution mechanics are separated. Application execution state and result metadata remain backend concerns.

## Compatibility requirements

Preserve SQL operations for concept queries, logical nodes, external IDs, date restrictions, secondary IDs, reused queries, table exports, and absolute/relative forms. Preserve dialect-specific behavior for the currently supported HANA and ClickHouse paths.

Preserve numeric types, null handling, range bounds, distinctness, subtraction, quarter counting, interval packing, aliases, projection order, and result interpretation. Table-export operations must retain their row-level semantics rather than accidentally adopting grouped-query semantics.

New resolved-model validation must not reject a previously supported request or change when application errors are reported. Backend adapters must preserve defaults and resolution semantics. Known quirks receive characterization coverage rather than fixes in this change.

Remove superseded SQL implementations only after the production backend delegates to the extracted implementation and compatibility checks pass. Completion must not leave two independently maintained implementations of the same SQL algorithm.

## Verification

1. Establish the existing test baseline and characterize behavior-sensitive paths before replacing their implementation. Compare extracted output with the existing implementation, using equivalent resolved inputs and dialect settings.
2. Run connector tests and affected backend tests. Exercise aggregations, selects, forms, and export paths; include nulls, duplicate events, distinct sum with subtraction, date boundaries, and duration distinctness.
3. Use existing SQL integration fixtures for result-level verification on supported databases where infrastructure is available. SQL text checks alone do not establish result parity. Record unavailable integration infrastructure explicitly.
4. Verify module dependencies and source imports contain no backend or framework implementation dependencies in the connector.
5. Assemble the extraction in an isolated checkout based on the recorded local `develop` revision. Apply only extraction changes and required neutral module/build additions. Run the module and Dropwizard build/test checks with no Quarkus modules present. Do not infer portability solely from a selected-module build on the migration branch.

## Review and portability

Keep extraction changes grouped by responsibility: shared vocabulary/build wiring, compiler infrastructure, operations, orchestration/dialects, and backend integration/execution. Avoid formatting unrelated files. Document original-to-extracted class mappings where names or responsibility splits are unavoidable.

Keep Quarkus changes out of extraction commits. Shared-file edits, especially the root POM, must be separable at the hunk level. Record the extraction commit/patch set and any application conflicts encountered during the `develop` verification. Do not rebase or rewrite the current migration branch to perform that check.

## Completion criteria

The Dropwizard backend uses the extracted SQL implementation for its supported paths; application-specific adapters are the remaining backend boundary. Compatibility checks pass without intended behavior changes, code remains recognizable, and an extraction-only checkout based on `develop` builds without knowledge of Quarkus. Any unverified database checks are reported as limitations rather than claimed successes.
