# SQL Connector Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete a recognizable, behavior-preserving SQL connector extraction that the Dropwizard backend can use on a branch based on `develop` without Quarkus.

**Architecture:** Move existing SQL algorithms into `sql-connector`; retain backend model resolution, configuration, lifecycle, and application result/error adaptation in `backend`. Reuse the existing resolved model and compiler infrastructure. Separate pure SQL and JDBC mechanics from application ownership without rewriting algorithms.

**Tech Stack:** Java 21, Maven wrapper, jOOQ 3.20.2, Jakarta Bean Validation with Hibernate Validator, JUnit Jupiter, existing HANA and ClickHouse integration suites.

**Spec:** `docs/superpowers/specs/2026-09-21-sql-connector-extraction-design.md`

## Global Constraints

- Preserve existing SQL behavior, including quirks.
- Preserve recognizable class names, method structure, SQL expressions, and algorithm organization wherever the dependency boundary permits.
- Strict validation at the resolved-model boundary is an explicit exception.
- No backend, Dropwizard, or Quarkus implementation dependencies in the connector.
- No dependency upgrades or unrelated cleanup.
- Preserve the current working changes; inspect them rather than resetting them.
- Work on the current migration branch; use an isolated checkout only for the later `develop` portability check.
- Do not run HANA or ClickHouse integration suites here. Prepare an external test handoff.
- Stage only the files belonging to each extraction step. Every commit must include the user-required `Co-authored-by: Codex <current model name> <codex@openai.com>` trailer after a blank line.

## Review Focus

1. Integer, floating-point, and money inputs must retain their existing jOOQ field types and null arithmetic (Task 1).
2. Select/filter combinations must not recompute a distinct or interval-packed aggregate after joining its predecessor (Tasks 1–2).
3. Reused queries, secondary IDs, and multiple connector branches must preserve aliases and result ordering (Task 4).
4. Table-export filters operate on individual rows; form date windows and feature/outcome columns retain their existing meaning (Task 5).
5. JDBC SQL NULL, empty lists, date bounds, exceptions, and early stream closure must preserve existing decoding and resource ownership (Task 6).

## File map and sequencing

Paths below are repository-relative. Execute tasks in order because dialect, operation, and orchestration contracts overlap.

- Shared compiler code: `sql-connector/src/main/java/com/bakdata/conquery/sql/`.
- Existing SQL implementation and backend adapters: `backend/src/main/java/com/bakdata/conquery/sql/`.
- Connector tests mirror the main package under `sql-connector/src/test/java/`.
- New backend compatibility tests live under `backend/src/test/java/com/bakdata/conquery/sql/` and use the `SqlExtraction*Test` prefix, allowing a safe explicit local test selection.
- Integration fixtures remain under `backend/src/test/resources/tests/`; retain expected outputs.
- `dataset-model` receives only shared vocabulary required by these moves. Do not relocate application domain models to avoid defining an adapter.
- Preserve existing public adapter signatures during each intermediate commit so the Dropwizard backend keeps compiling.

## Local verification commands

Run from the repository root in PowerShell. Inspect exit codes and Surefire reports; a zero-test run is not verification.

```powershell
.\mvnw.cmd -B -ntp -pl sql-connector -am test
.\mvnw.cmd -B -ntp -pl backend -am '-DskipTests' package
.\mvnw.cmd -B -ntp -pl backend -am '-Dtest=SqlExtraction*Test' '-Dsurefire.failIfNoSpecifiedTests=false' test
git diff --check
```

The backend package command compiles tests but does not execute them. The explicit test selector executes only new local compatibility tests; do not replace it with an unrestricted backend test run. Run the third command only after those tests exist. Record any pre-existing build failures separately before changing code.

### Task 1: Complete aggregation extraction with compatibility coverage

**Files:**
- Modify the pending `compiler/conversion/operation/{ResolvedAggregationConverter,SumAggregationConverter,DateAggregationConverters,BuiltInAggregationConverters,BuiltInAggregationFilterConverters,AggregationConversionContext}.java` in the connector.
- Modify `compiler/conversion/operation/ResolvedFilterConverter.java` and its tests.
- Modify `compiler/conversion/operation/ResolvedAggregationConverterTest.java` in connector tests.
- Adapt backend `conversion/model/aggregator/{SumSqlAggregator,CountSqlAggregator,CountQuartersSqlAggregator,DurationSumSqlAggregator,DateDistanceSqlAggregator,FlagSqlAggregator}.java` as each operation moves.
- Create backend test `sql/conversion/SqlExtractionAggregationTest.java`.
- Update `sql-connector/README.md` to remove claims of intended behavior changes.

**Interfaces:** Retain `ResolvedAggregationConverter.convert(ResolvedAggregation, AggregationConversionContext): CommonAggregationSelect<?>`. Backend adapters resolve columns and construct operations; shared converters produce the existing aggregation IR. Preserve the additional-predecessor contract in `CommonAggregationSelect`.

- [ ] Run the connector baseline and backend compilation commands. Record existing failures without attributing them to this task.
- [ ] Read each original aggregator alongside the pending implementation. Record numeric field classes from `NumberMapUtil`, projection ordering, date conversion, and table-export behavior before replacing any backend method.
- [ ] Replace the pending test that expects subtraction inside distinct sums with this characterization, using the existing test class's constants and helpers:

```java
@Test
void shouldIgnoreSubtractionInsideDistinctSumLikeExistingBackend() {
    var withoutSubtract = converter.convert(
            new BuiltInAggregations.Sum(AMOUNT, Optional.empty(), List.of(PERSON)), CONTEXT);
    var withSubtract = converter.convert(
            new BuiltInAggregations.Sum(AMOUNT, Optional.of(DISCOUNT), List.of(PERSON)), CONTEXT);
    assertEquals(renderRootSelects(withoutSubtract), renderRootSelects(withSubtract));
    assertEquals(render(withoutSubtract.getGroupBy().select()), render(withSubtract.getGroupBy().select()));
}
```

- [ ] Run `ResolvedAggregationConverterTest` via Maven with `-pl sql-connector -am`, `-Dtest=ResolvedAggregationConverterTest`, and `-Dsurefire.failIfNoSpecifiedTests=false`; confirm the new test fails because subtraction is included.
- [ ] Extract the original distinct-sum helper structure: project the sum and distinct-key columns, assign row numbers by IDs/keys, filter row number 1, and sum the original value with its original coalesce expression. Do not pass the subtraction column into this branch.
- [ ] Add compatibility cases for ordinary sum with zero/one/two null operands, INTEGER/REAL/DECIMAL/MONEY source types, count with multiple distinct keys, single-date versus date-pair quarter counts, and packed/unbounded durations. Compare rendered expressions and predecessor structure with expectations derived from the original backend implementation.
- [ ] Inspect the original duration `distinctBy` path. Preserve its SQL behavior for valid inputs; express any unsupported resolved-model combination as a declarative boundary constraint with a validation test instead of leaving an incidental exception deep in compilation. Document the chosen boundary contract in the README.
- [ ] Move remaining aggregation SQL with the original helper names where practical; keep column resolution and filter/select DTO adaptation in the backend. Verify that an additional predecessor supplies an already-computed aggregate and is not aggregated again.
- [ ] Run connector tests, the backend aggregation compatibility test, and backend compilation. Review the diff for unintended expression/type changes, then commit this task alone.

### Task 2: Extract connector and concept selects

**Files:**
- Move SQL bodies from backend `conversion/model/select/{ValueSelectUtil,RandomValueSelectConverter,LastValueSelectConverter,FirstValueSelectConverter,ExistsSelectConverter,EventDurationSumSelectConverter,EventDateUnionSelectConverter,DistinctSelectConverter,DateUnionSelectConverter,DaterangeSelectUtil,ConceptColumnSelectConverter}.java` into the connector's `compiler/conversion/operation/` package, preserving class/helper names where responsibilities match.
- Create connector `compiler/conversion/operation/ResolvedSelectConverter.java` and `SelectConversionContext.java`.
- Test in connector `compiler/conversion/operation/ResolvedSelectConverterTest.java` and backend `sql/conversion/SqlExtractionSelectTest.java`.

**Interfaces:** `ResolvedSelectConverter.convert(ResolvedSelect, SelectConversionContext): ConnectorSqlSelects` for connector-level outputs. Keep concept-level selection composition separate where the existing backend uses a different result type; reuse its existing IR rather than forcing both paths into one shape. Reuse Task 1's aggregation converter. `SelectConversionContext` carries the resolved table/ID state, compiler dialect, name generator, and alias currently read by these select helpers, without backend DTOs.

- [ ] Characterize every built-in select represented by `BuiltInSelects`: aggregation, values (distinct/first/last/random), date union/distance, concept values, event date union/duration, and exists. Pin each operation's SQL, required input columns, and final projection order before moving it.
- [ ] For first/last/random, retain existing ordering and tie behavior. For date distance, use the producer-frozen end date; do not introduce a compiler clock. For substring selection, preserve index/range conventions.
- [ ] Add failing cases for missing dispatch registrations and mixed aggregation/value selects with an additional predecessor. Use fixed aliases and fixed dates to make SQL comparisons deterministic; do not assert a particular database result for inherently random/tied choices.
- [ ] Move the original SQL expressions and helper bodies, replacing backend column resolution with `ResolvedColumn` inputs. Backend select adapters delegate to the moved code.
- [ ] Run connector select tests, the backend select compatibility test, and backend compilation. Commit only the select extraction and associated coverage.

### Task 3: Move dialect SQL capabilities

**Files:**
- Backend sources: `conversion/dialect/{SqlFunctionProvider,LegacyCompilerDialect,DialectBundle}.java`, `conversion/dialect/hana/{HanaSqlFunctionProvider,HanaStratificationFunctions,HanaDialectBundle}.java`, and `conversion/dialect/clickhouse/{ClickhouseFunctionProvider,ClickhouseStratificationFunctions,ClickhouseDistinctSelectConverter,ClickhouseDialectBundle}.java`.
- Move reusable providers and stratification functions into connector `compiler/dialect/`, retaining vendor subpackages and existing class names.
- Modify connector `compiler/dialect/CompilerDialect.java`.
- Create backend test `sql/conversion/SqlExtractionDialectTest.java`.

**Interfaces:** `CompilerDialect` is the shared capability boundary. `DialectBundle` remains backend composition for configuration, datasource checks, result processors, and application type compatibility. Shared dialects must not register converters accepting backend `Visitable` or `Select` types.

- [ ] Capture HANA/ClickHouse rendering expectations for date min/max, range construction, date arithmetic, quarters, arrays, concatenation, distinct selects, literal/no-op tables, and stratification expressions without opening a connection.
- [ ] Move provider methods with unchanged jOOQ expressions; adapt `CDateRange` and application enums at the backend boundary. Preserve vendor-specific native date-range behavior; do not substitute a generic dual-column implementation merely because the neutral model makes it easier.
- [ ] Remove backend references from the moved classes and wire backend dialect bundles to delegate. Avoid adding default implementations that silently substitute unsupported vendor behavior.
- [ ] Run rendering compatibility tests and backend compilation. Commit dialect moves separately from algorithm changes.

### Task 4: Connect resolved concept queries to the shared compiler

**Files:**
- Create connector `compiler/DefaultSqlCompiler.java` implementing existing `SqlCompiler`.
- Modify connector `compiler/conversion/` and `compiler/ir/concept/` to connect existing condition, filter, select, and CTE components.
- Create backend `conversion/ResolvedQueryAdapter.java`.
- Adapt backend `conversion/{SqlConverter,NodeConversions}.java`, `conversion/cqelement/` converters, `conversion/query/{ConceptQueryConverter,CQReusedQueryConverter,SecondaryIdQueryConverter}.java`, and `conversion/model/SqlQuery.java`.
- Create connector test `compiler/DefaultSqlCompilerTest.java` and backend test `sql/conversion/SqlExtractionQueryTest.java`.

**Interfaces:** Preserve `SqlCompiler.compile(ResolvedQuery, CompilerDialect): CompiledQuery`. The backend adapter produces the existing `ResolvedQuery` from the already-resolved application query/context. Invoke `ResolvedQueryValidation.validate(ResolvedQuery)` once before compilation using the application's validator. Map ordered `CompiledColumn` metadata back to existing `ResultInfo` at the backend boundary.

- [ ] Build local compatibility fixtures for all-entities, AND/OR/negation, external IDs, date restrictions, concepts with multiple connector branches, reused queries, and secondary IDs. Include repeated names, empty optional selections, and validity-date output.
- [ ] Assert ordered output IDs/types/aliases as well as rendered SQL. Add a validation-boundary test proving invalid nested operations are rejected before compiler invocation.
- [ ] Move traversal and composition into the connector using the existing logical/external/final/concept CTE helpers. Preserve the current name-generator lifetime and alias rules rather than creating a new naming convention.
- [ ] Implement backend adaptation: expand saved references, resolve physical columns and IDs, materialize defaults/date actions, freeze dates, and retain presentation metadata outside compiler internals. Do not query repositories during compilation.
- [ ] Route concept execution through the shared compiler. Retain backend wrappers needed by forms/export until Task 5 replaces their remaining SQL bodies; do not maintain a second complete concept compiler.
- [ ] Run connector tests and the explicit backend compatibility suite; compile the backend and commit this integration.

### Task 5: Extract form and export SQL without changing their contracts

**Files:**
- Backend sources: `conversion/query/{AbsoluteFormQueryConverter,RelativFormQueryConverter,FormConversionHelper,EntityDateQueryConverter,TableExportQueryConverter}.java` and `conversion/forms/`.
- Move pure form SQL helpers into connector `compiler/forms/` and export SQL helpers into `compiler/conversion/query/`, keeping the existing helper/class names.
- Add resolved input records adjacent to those helpers for the values currently read from application form/export DTOs; reuse `DateRange`, resolved columns, existing query-step IR, and moved form vocabulary.
- Create backend `sql/conversion/SqlExtractionFormTest.java` and `SqlExtractionExportTest.java`.

**Interfaces:** Keep `ResolvedQuery` and `SqlCompiler` as the concept-query contract. Do not widen `ResolvedQuery` with unrelated optional form/export fields. Form helpers consume resolved dates, stratification settings, feature steps, and dialect functions; export helpers consume resolved columns and row predicates. Existing application converter entry points remain backend adapters.

- [ ] Pin original absolute/relative form SQL and output order for empty windows, boundary dates, complete resolution, and feature/outcome scopes. Include two feature groups and secondary IDs to expose join/alias collisions.
- [ ] Pin table-export filter expressions separately from aggregation filters, including sum subtraction with null operands, duration distance, select filters, and date restrictions. Expected expressions come from the existing `convertForTableExport` methods.
- [ ] Move the original stratification, form join, and projection helper bodies. Replace application enum/config lookups with explicitly resolved values; keep the original algorithm and helper decomposition.
- [ ] Move row-level export expressions and route backend adapters to them. Do not reuse grouped filters where existing table exports use plain arithmetic or predicates.
- [ ] Run both compatibility classes, connector tests, and backend compilation. Commit form and export moves in separate commits when their diffs are independent.

### Task 6: Extract reusable JDBC and result decoding mechanics

**Files:**
- Backend sources: `execution/{SqlExecutionService,ResultSetProcessor,DefaultResultSetProcessor,SqlCDateSetParser,DefaultCDateSetParser,HanaSqlCDateSetParser,SqlEntityResult,SqlExecutionExecutionInfo}.java` and `conversion/dialect/clickhouse/ClickhouseResultSetProcessor.java`.
- Move reusable processor/parser/JDBC bodies into connector `execution/`; preserve existing names where a backend wrapper is unnecessary.
- Adapt backend `conquery/SqlExecutionManager.java` and dialect bundle wiring.
- Create backend test `sql/execution/SqlExtractionExecutionTest.java` and connector tests beside the moved execution classes.

**Interfaces:** Keep application `ResultInfo`, `EntityResult`, `ExecutionState`, and `ConqueryError` in backend adapters. Retain the JDBC reader contract `T read(ResultSet, int) throws SQLException`. Shared execution consumes SQL and ordered readers and exposes raw row values; backend code owns conversion into application results and errors. Retain existing stream ownership and close behavior.

- [ ] Characterize JDBC decoding using controlled `ResultSet`/connection doubles: SQL NULL versus zero, empty versus null lists, money precision, date sentinels, column offsets, and multiple result columns.
- [ ] Characterize statement/result-set closure on success, decoding failure, and SQL exceptions. Cover closing an execution stream before exhaustion. Preserve existing ownership of caller-supplied connections.
- [ ] Move decoding and execution loops without changing conversion expressions or resource scopes. Keep backend `readerForType` dispatch if it depends on application `ResultType`; delegate to shared reader methods rather than importing the application type into the module.
- [ ] Preserve backend error wrapping and execution state transitions. Run local execution tests, all connector tests, and backend compilation. Commit the extraction.

### Task 7: Remove obsolete SQL implementations and verify the dependency boundary

**Files:** Backend adapters touched in Tasks 1–6, `sql-connector/README.md`, `sql-connector/pom.xml`, `dataset-model/pom.xml`, `backend/pom.xml`, and root `pom.xml` only where necessary.

- [ ] Search backend SQL classes for remaining jOOQ construction. Classify each remaining expression as application adaptation or reusable SQL; move remaining reusable bodies before declaring extraction complete.
- [ ] Remove superseded implementations after checking callers. Preserve public backend configuration and API schemas.
- [ ] Inspect all connector imports and Maven dependency trees for backend/framework dependencies. Check the resolved graph, not just direct POM dependencies:

```powershell
.\mvnw.cmd -B -ntp -pl sql-connector -am dependency:tree
rg -n '^import .*?(quarkus|dropwizard|conquery\.(apiv1|models\.config|models\.identifiable|models\.query))' sql-connector/src/main/java
```

- [ ] Document each moved/split class and its backend adapter in the connector README. Document strict boundary validation and preserved SQL quirks, including distinct-sum subtraction.
- [ ] Run all local verification commands. Review the extraction diff for unrelated changes and Quarkus edits; commit cleanup and documentation.

### Task 8: Verify extraction portability and prepare external tests

**Files:** Create `docs/sql-connector-extraction-verification.md`; edit POM module lists only in the isolated extraction checkout as required to apply extraction-only changes.

- [ ] Record `git rev-parse develop`, the migration branch revision, working-tree status, and extraction commit list. Never reset or rebase the migration checkout.
- [ ] Use the worktree skill to create an isolated checkout from the recorded `develop` revision. Apply the extraction commits/patches, including earlier extraction work already present before this plan. Inspect shared-file hunks so Quarkus module entries and migration-only dependencies are omitted.
- [ ] Resolve portability conflicts using the unchanged Dropwizard behavior as the reference. Record any prerequisite neutral changes explicitly. Do not copy the complete migration root POM.
- [ ] Confirm no Quarkus sources/modules/configuration are present. Run the connector suite, explicit backend compatibility suite, and backend package command from this checkout. Then package the existing Dropwizard reactor with tests skipped to catch downstream compilation failures.
- [ ] Write the exact verified local commands, revisions, results, and omitted integration checks in the verification document. Include reports under each module's `target/surefire-reports`.
- [ ] Prepare these external Linux commands for the extraction revision, after verifying the test selectors still match the repository. HANA container mode requires a suitable Linux/Docker host and the suite's `/tmp/data/hana` mount; ClickHouse requires Docker. Use the repository's existing provider configuration for remote database mode, and document required environment-variable names without recording credentials.

```bash
USE_LOCAL_HANA_DB=true ./mvnw -B -ntp -pl backend -am \
  -Dtest=HanaSqlIntegrationTests -Dsurefire.failIfNoSpecifiedTests=false test
USE_LOCAL_CH_DB=true ./mvnw -B -ntp -pl backend -am \
  -Dtest=ClickhouseSqlIntegrationTests -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] Ask the user to return the exact tested revision, command exit statuses, and Surefire reports from the other machine. Keep result-level compatibility marked pending until those results arrive; do not replace these databases with an in-memory database and claim parity.
- [ ] Commit the verification/handoff document. Report changes in logical review order, local verification evidence, extraction portability evidence, and pending external checks.

## Plan self-review

The tasks cover the spec's operations, dialects, orchestration, execution, strict validation, recognizable code, and `develop` portability. Review Focus cases are assigned to Tasks 1–6. The plan intentionally preserves the existing concept compiler signature and treats form/export helpers as separate contracts. External database verification remains a separate, explicit acceptance item; implementation can proceed without those databases locally.
