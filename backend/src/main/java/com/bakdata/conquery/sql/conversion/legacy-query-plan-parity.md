# SQL backend parity with the legacy QueryPlan backend

This document records known feature gaps and behavioral differences between the
legacy in-memory `QueryPlan` backend and the SQL-generating backend. It is based
on the converter registry, both implementations of the affected operations, and
the integration-test metadata as of commit `d6e3a4659` (2026-09-24).

The statuses used below are:

- **Missing**: a valid legacy query reaches `No converter found`, an inherited
  `UnsupportedOperationException`, or another explicit not-implemented path.
- **Mismatch**: SQL conversion exists, but its result can differ from the
  `QueryPlan` result.
- **Unverified**: code exists, but the corresponding legacy fixture is disabled
  for SQL or is limited to one dialect. This is not by itself proof of a defect.

## Executive summary

The SQL backend does not yet provide general drop-in parity. The most important
gaps are:

1. temporal queries and standalone array queries have no registered converter;
2. several selects, filters, and concept-tree conditions have no SQL
   implementation;
3. table export only partially implements the legacy row-selection and concept
   value behavior;
4. several implemented aggregations ignore configuration (`distinctBy`,
   subtraction, or substring ranges);
5. conversion state is represented by mutable-looking values on a shared
   context, causing nested-query differences; and
6. ClickHouse has substantially less form/export coverage than HANA.

## Missing query and CQElement converters

The default registry in
[`DialectBundle#getDefaultNodeConverters`](dialect/DialectBundle.java) is the
authoritative list for both SQL dialects.

| Legacy feature | SQL status | Observable result |
| --- | --- | --- |
| Standalone `ArrayConceptQuery` | **Missing** | Forms manually iterate the child queries in `FormConversionHelper`, but submitting an `ARRAY_CONCEPT_QUERY` directly has no node converter and fails with `No converter found`. The legacy backend executes it with `ArrayConceptQueryPlan`. |
| `CQTemporal` (`TEMPORAL`) | **Missing** | There is no SQL node converter. The legacy implementation evaluates index periods, samples dates, and evaluates the comparison query per selected period through `TemporalQueryNode`. All 50 temporal SQL fixtures are disabled. |

Relevant implementations:

- [`ArrayConceptQuery`](../../apiv1/query/ArrayConceptQuery.java)
- [`ArrayConceptQueryPlan`](../../models/query/queryplan/ArrayConceptQueryPlan.java)
- [`CQTemporal`](../../apiv1/query/concept/specific/CQTemporal.java)
- [`TemporalQueryNode`](../../models/query/queryplan/specific/temporal/TemporalQueryNode.java)
- [`FormConversionHelper`](query/FormConversionHelper.java)

## Missing selects and filters

`Select#createConverter()` and `Filter#createConverter()` throw by default.
Consequently, a subtype without an override is unsupported even if its result
reader happens to be implemented.

| Type | Kind | SQL status / evidence |
| --- | --- | --- |
| `PREFIX` (`PrefixSelect`) | select | **Missing.** It has no converter override and its result reader explicitly says that PREFIX is not implemented in SQL mode. |
| `QUARTER` (`QuarterSelect`) | select | **Missing.** No converter override; its SQL fixture is disabled as “Not implemented yet.” |
| `QUARTERS_IN_YEAR` (`QuartersInYearSelect`) | select | **Missing.** No converter override. |
| `PREFIX_TEXT` (`PrefixTextFilter`) | event filter | **Missing.** No converter override. |
| `QUARTERS_IN_YEAR` (`QuartersInYearFilter`) | aggregation filter | **Missing.** No converter override and its SQL fixture is disabled. |

See [`Select`](../../models/datasets/concepts/select/Select.java),
[`Filter`](../../models/datasets/concepts/filters/Filter.java), and the
corresponding disabled fixtures under
[`tests/aggregator`](../../../../../../../test/resources/tests/aggregator) and
[`tests/filter`](../../../../../../../test/resources/tests/filter).

## Concept-tree condition gaps

The SQL backend constructs a `ConceptIdMapping` for every converted
`CQConcept`, and that mapping recursively builds an expression for every child
in the concept tree. Unsupported child conditions can therefore fail a query
even when the query selects the root or a different child.

| Condition | Connector predicate | Concept ID mapping | Status |
| --- | --- | --- | --- |
| `GROOVY` | unsupported | unsupported | **Missing.** Both SQL methods throw. |
| `PREFIX_LIST` | implemented as a regex predicate | unsupported | **Missing for tree mappings.** `buildExpression()` throws. |
| `PREFIX_RANGE` | implemented as a regex predicate | unsupported | **Missing for tree mappings.** `buildExpression()` throws. |
| `NOT` | implemented by negating the child predicate | unsupported | **Missing for tree mappings.** `buildExpression()` throws. |

Evidence:

- [`ConceptIdMapping`](cqelement/concept/ConceptIdMapping.java)
- [`GroovyCondition`](../../models/datasets/concepts/conditions/GroovyCondition.java)
- [`PrefixCondition`](../../models/datasets/concepts/conditions/PrefixCondition.java)
- [`PrefixRangeCondition`](../../models/datasets/concepts/conditions/PrefixRangeCondition.java)
- [`NotCondition`](../../models/datasets/concepts/conditions/NotCondition.java)

The four concept-query fixtures whose comments say “Prefix not supported in SQL
Mode” and the Groovy fixture are disabled for this reason.

## Confirmed aggregation and filter mismatches

### `distinctBy`

| Feature | Legacy behavior | SQL behavior |
| --- | --- | --- |
| `CountSelect` / `CountFilter` with `distinctByColumn` | De-duplicates by the configured tuple of columns before counting the value column. | `CountSqlAggregator` only uses `countDistinct(valueColumn)` and never reads `distinctByColumn`. |
| `DurationSumSelect` with `distinctBy` | Wraps the duration aggregator in `DistinctValuesWrapperAggregator`. | `DurationSumSqlAggregator` never reads `distinctBy`; the source contains an explicit TODO and its fixture is disabled. |
| `SumSelect` / `SumFilter` with both `distinctByColumn` and `subtractColumn` | De-duplicates rows and sums `column - subtractColumn`. | The SQL distinct branch only selects and sums `column`; `subtractColumn` is ignored. |

Implementations:

- [`CountSqlAggregator`](model/aggregator/CountSqlAggregator.java)
- [`DurationSumSqlAggregator`](model/aggregator/DurationSumSqlAggregator.java)
- [`DurationSumSelect`](../../models/datasets/concepts/select/connector/specific/DurationSumSelect.java)
- [`SumSqlAggregator`](model/aggregator/SumSqlAggregator.java)

### Substring ranges

The legacy backend applies configured substring ranges before comparing or
returning values. SQL currently differs in two places:

- `MultiSelectFilter` and `SingleSelectFilter`: `AbstractSelectFilterConverter`
  compares the complete column and does not inspect `SelectFilter.substringRange`.
  The `SELECT_SUBSTRING` SQL fixture is disabled.
- `RandomValueSelect`: the SQL converter aggregates the complete root column and
  does not apply `getSubstringRange()`. First, last, and distinct selects do use
  the shared substring conversion.

See [`AbstractSelectFilterConverter`](model/filter/AbstractSelectFilterConverter.java),
[`SelectFilter`](../../models/datasets/concepts/filters/specific/SelectFilter.java),
and [`RandomValueSelectConverter`](model/select/RandomValueSelectConverter.java).

### Quarter counting

`CountQuartersSqlAggregator` documents that its two-column range path sums the
quarter count per event and does not remove overlap between events. The intended
meaning is a count of distinct quarters, so overlapping ranges can be counted
more than once. Note that the legacy select/filter itself only implements the
single-date-column path; the SQL-only two-column extension is therefore both
incorrect and not a parity implementation.

See [`CountQuartersSqlAggregator`](model/aggregator/CountQuartersSqlAggregator.java)
and [`CountQuartersSelect`](../../models/datasets/concepts/select/connector/specific/CountQuartersSelect.java).

### Date-distance filters and units

`DateDistanceAggregator` computes the minimum distance over all matching events
and the legacy `RangeFilterNode` tests that aggregate. The SQL filter converter
instead installs a per-event predicate. For distances `[1, 10]` and a requested
range `[5, 15]`, for example, legacy rejects the entity because the minimum is
`1`, while SQL discards that event, retains `10`, and accepts the entity. The SQL
select path does correctly apply `min`, so select and filter semantics also
differ from one another.

The API accepts a `ChronoUnit`. Legacy delegates to `ChronoUnit.between`, whereas
both SQL dialects only implement days, months, years, decades, and centuries.
Valid date units such as weeks and millennia therefore work in the legacy path
but throw during SQL generation. SQL also narrows the result to `Integer` while
the legacy aggregator uses `Long`.

See [`DateDistanceSqlAggregator`](model/aggregator/DateDistanceSqlAggregator.java)
and [`DateDistanceAggregator`](../../models/query/queryplan/aggregators/specific/DateDistanceAggregator.java).

### First, last, and random value selection

- Legacy random selection is reproducible because `RandomValueAggregator` uses
  `ConqueryConstants.RANDOM_SEED`; the SQL aggregate uses the database random
  function and is not reproducible across executions.
- For first/last selection without a validity date, legacy returns the first
  encountered event. SQL orders a window only by the entity ID, which is equal
  for all candidate rows; the selected row is database-plan dependent.
- With equal validity-date bounds, legacy deliberately retains the first
  encountered event. SQL has no stable tie-breaker and may choose another row.

See [`RandomValueAggregator`](../../models/query/queryplan/aggregators/specific/value/RandomValueAggregator.java)
and [`ValueSelectUtil`](model/select/ValueSelectUtil.java).

## Concept-value selection mismatch

`ConceptColumnSelectConverter` intentionally reads **all connectors of the
concept**, including connectors absent from the submitted `CQConcept`. For an
absent connector it reads the root SQL table directly, bypassing the submitted
table filters, date restriction, and the normal preprocessing/event-filter
steps. The legacy aggregator only consumes events visited by its query-plan
node. This can add values that did not contribute to the match.

Both `CONCEPT_VALUES` fixtures are disabled for SQL. The SQL implementation also
contains an unresolved `TODO` on whether the union should carry negation.

See [`ConceptColumnSelectConverter`](model/select/ConceptColumnSelectConverter.java).

## Table export is only partially equivalent

[`TableExportQueryPlan`](../../models/query/queryplan/TableExportQueryPlan.java)
evaluates the complete `CQConcept` node once per exported event. In contrast,
[`TableExportQueryConverter`](query/TableExportQueryConverter.java) selects
directly from each connector table and applies only the explicit `CQTable`
filters. This creates several differences:

1. **Concept selection is ignored.** Selected concept elements, tree conditions,
   and connector conditions are not applied to exported rows.
2. **`rawConceptValues=false` is ignored by SQL generation.** SQL always selects
   the physical concept column. The result metadata installs a
   `ConceptIdPrinter`, which expects the legacy integer local ID, so a physical
   string code can also fail during rendering rather than merely print the wrong
   value.
3. **The source column differs.** Legacy emits the table label; SQL emits the
   physical table name.
4. **Dateless tables are unsafe.** Legacy skips the date-range check when a
   connector has no validity date. `joinConnectorTableWithPrerequisite()` calls
   `forValidityDate(cqTable.findValidityDate())` without a null branch.
5. **Some aggregation filters are reduced incorrectly to a row predicate.** For
   example, count and count-quarters use the constant `1`; unlike legacy's
   single-event aggregator evaluation, this does not check whether the counted
   column is null and cannot represent a date range spanning several quarters.
6. **ClickHouse remains unverified/broken.** The raw export fixture is restricted
   to HANA with the comment “Bug in Clickhouse converter.” The non-raw fixture is
   disabled for SQL entirely.

## Nested conversion-state mismatches

`ConversionContext` stores negation, date restriction, selected secondary ID,
external extras, and generated steps in one value object. Some converters reset
or return those fields as if they were a stack, but they do not restore the
parent value in all cases.

### Double negation

`CQNegationConverter` always sets `negation=true` for its child and then resets
it to false. A nested negation therefore does not toggle the state: `NOT NOT X`
is converted as a negated `X`, while the legacy `NegatingNode` composition
restores positive membership.

### Nested date restrictions

Legacy `DateRestrictingNode` intersects its restriction with the active parent
restriction at execution time. `CQDateRestrictionConverter` instead replaces
`dateRestrictionRange` with the child's range and unconditionally clears it to
null afterwards. Nested restrictions therefore use only the innermost range in
SQL instead of their intersection. The same reset pattern would also discard a
non-null parent range when returning through reused/nested components.

### External extra columns under logical nodes

`CQExternalConverter` stores extra columns in `ConversionContext.externalExtras`.
`CQAndConverter` and `CQOrConverter` return the caller's context with only the
joined `QueryStep`; they do not propagate the converted child context. Therefore
extras work for a root `CQExternal` but are lost when that external node is
wrapped in AND/OR. Multiple external nodes also share a single `externalExtras`
slot, so later values overwrite earlier ones.

### Reused-query secondary-ID scope

For `CQReusedQuery.excludeFromSecondaryId`, the legacy code shadows the selected
secondary ID in a local `QueryPlanContext` variable. The SQL converter returns
the converted context with `secondaryIdDescription=null`. During sequential
sibling conversion that null can leak into following siblings, so they stop
grouping by the secondary ID too. Existing programmatic tests assert result
counts but not the generated result schema, and `SqlExecutionService` assumes
that every column after the primary ID has a matching `ResultInfo`, so this kind
of leak is not validated centrally.

Relevant code:

- [`ConversionContext`](cqelement/ConversionContext.java)
- [`CQNegationConverter`](cqelement/CQNegationConverter.java)
- [`CQDateRestrictionConverter`](cqelement/CQDateRestrictionConverter.java)
- [`CQAndConverter`](cqelement/CQAndConverter.java)
- [`CQOrConverter`](cqelement/CQOrConverter.java)
- [`CQExternalConverter`](cqelement/CQExternalConverter.java)
- [`CQReusedQueryConverter`](query/CQReusedQueryConverter.java)
- [`SqlExecutionService`](../execution/SqlExecutionService.java)

## Date and form differences

### Incomplete `LOGICAL` date aggregation

The SQL date-aggregation machinery implements merge, intersect, and inversion,
but parity is incomplete for combinations of logical nodes, aggregation
selects, restrictions, and negation. Six focused `LOGICAL` fixtures are disabled
for SQL while simpler OR/negation cases are only enabled for HANA. Treat
`DateAggregationMode.LOGICAL` as partially supported, not generally equivalent.

### Relative form with a dateless prerequisite

Legacy `RelativeFormQueryPlan` returns a placeholder result row when the
prerequisite matches but no date can be sampled. SQL
`FormConversionHelper.convertPrerequisite()` installs `falseCondition()` when
the converted prerequisite has no validity-date column, removing the entity
entirely. `REL_EXPORT/PREREQUISITE_WITHOUT_DATES` is disabled for SQL.

### Form dialect coverage

All 14 form fixtures are restricted to HANA. The absolute and relative export
fixtures explicitly cite missing ClickHouse lateral-join support. This means the
presence of ClickHouse stratification code must not be interpreted as end-to-end
form parity.

### Unbounded HANA date ranges

The legacy date model supports positive infinity. HANA SQL conversion explicitly
throws when a `CDateRange` has `POSITIVE_INFINITY` as its upper bound. Queries
that rely on an open upper bound therefore require normalization or a dialect
implementation before they are portable.

See [`HanaSqlFunctionProvider`](dialect/hana/HanaSqlFunctionProvider.java).

## Preprocessing and result-shape limitations

- SQL mode does not run the legacy import/preprocessing pipeline. Virtual output
  columns such as `COMPOUND_DATE_RANGE` are not translated into SQL expressions;
  converters reference the configured column as if it physically existed. The
  compound-date-range fixture is disabled. Other `OutputDescription` types need
  the same explicit audit or must be materialized in the source database.
- Imports are explicitly unsupported in SQL mode (`FailingImportHandler`).
- Nested list result types are explicitly unsupported by `ResultType.ListT`'s
  SQL reader.
- ClickHouse schema validation currently reports every SQL field as type
  compatible, so type mismatches that HANA rejects early can reach query
  execution and fail or coerce differently.

## Test evidence and remaining blind spots

The JSON integration suite currently contains 207 `*.test.json` specs under
`backend/src/test/resources/tests`. Of these, 75 are explicitly disabled for
SQL: 50 temporal cases and 25 other cases (6 aggregator, 3 filter, and 16 other
query fixtures). Missing `sqlSpec` means enabled, so disabled fixtures are a
useful backlog but do not cover the silent mismatches above.

The non-temporal disabled fixtures cover these areas:

- standalone arrays and aggregate `EXISTS` over arrays;
- concept-column values;
- duration sum with `distinctBy`;
- prefix and quarter selects;
- prefix, quarters-in-year, and substring filters;
- compound date-range preprocessing;
- selected `LOGICAL` combinations;
- prefix and Groovy concept conditions;
- dateless relative-form prerequisites; and
- non-raw table export.

Programmatic tests default to worker-only until explicitly opted into SQL. At
present only filter autocomplete and reused-query tests opt into both modes, so
most API-level behavior is not a parity oracle.

## Recommended closure order

1. Add a cross-backend parity harness that executes the same spec in worker and
   SQL mode and compares typed rows, not only result counts or rendered CSV.
2. Fail fast with a supported-feature validator before SQL generation. This
   turns silent misexecution (`distinctBy`, substring, table export) into a clear
   creation error while features are implemented.
3. Implement/repair table export because it is used by full export and entity
   preview and currently has both filtering and rendering differences.
4. Fix aggregation configuration parity and add small focused fixtures for each
   combination (`distinctBy`, subtraction, substring, ties, overlapping dates).
5. Replace boolean/single-slot conversion state with scoped child contexts that
   restore parent values; add nested negation, external-extra, and reused-query
   secondary-ID tests.
6. Add `ArrayConceptQuery` and temporal converters, then enable the existing
   array and temporal fixtures as their acceptance suite.
7. Define and enforce a per-dialect capability matrix for forms, ranges,
   preprocessing outputs, and result types.
