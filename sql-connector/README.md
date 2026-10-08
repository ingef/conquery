# SQL connector

This module contains the framework-neutral contract between query resolution and SQL compilation.

## Resolved query model

`ResolvedQuery` is an in-memory execution model. A producer must resolve and validate a request against one dataset
catalog snapshot before constructing it. In particular:

- logical references are replaced with physical tables and typed columns;
- saved or reusable query references are expanded;
- defaults and date-aggregation actions are materialized;
- filter values, selects, conditions, validity dates, and secondary IDs are validated;
- result columns are ordered and their presentation metadata is finalized.

The SQL compiler may perform dialect capability checks, but it must not access a dataset repository or resolve logical
identifiers. Connections, dialect implementations, compiler state, and execution services are intentionally not part of
the resolved model.

The module uses top-level packages as architectural boundaries:

- `model` contains only the immutable, fully resolved compiler input;
- `validation` contains Bean Validation constraints and the explicit validation boundary;
- `compiler` contains framework-neutral compilation contracts, including the public dialect capabilities;
- SQL conversion and execution implementations belong in sibling packages, not below `model`.

The resolved model is organized by responsibility:

- `model.node` contains the normalized query tree;
- `model.operation` contains the open operation interfaces and standard implementations;
- `model.schema` contains resolved physical tables, columns, connectors, and entity metadata;
- `model.range` and `model.result` contain shared value objects.

Shared dataset vocabulary such as `ColumnType` comes from the dependency-free `dataset-model` module. SQL-specific
physical metadata remains in `model.schema`.

`ResolvedFilter`, `ResolvedSelect`, `ResolvedCondition`, and `ResolvedAggregation` are open extension points.
Implementations must be immutable and carry resolved columns and typed values. They must not carry unresolved repository
identifiers or arbitrary SQL received from a query request.

The `BuiltInFilters`, `BuiltInSelects`, `BuiltInConditions`, and `BuiltInAggregations` types define the normalized
operations understood by the standard compiler. They intentionally describe semantics rather than the configuration
classes used by a particular query-producing application.

## Aggregation compilation

`ResolvedAggregationConverter` compiles count, sum, quarter-count, and duration-sum operations. It accepts additional
converters for extension aggregation types. `ResolvedFilterConverter` uses this dispatcher for numeric
`BuiltInFilters.AggregationRange` filters, including custom numeric aggregations.

Ordinary aggregates are projected in the connector's aggregation CTE. Distinct sums and interval-packed duration sums
are computed in separate predecessor CTEs; the range predicate reads their result after the branches are joined.
An aggregation returned with an `additionalPredecessor` must already be computed in that predecessor.

Sum subtraction preserves missing values when both operands are null. Distinct sums preserve the existing behavior and
ignore the subtraction column. Numeric input expressions retain their existing integer, floating-point, or money types.
Quarter counts preserve the legacy behavior: distinct
year-quarter keys for single dates, and a sum of each event's quarter count for date pairs, including overlaps.
Duration sums pack overlapping intervals first and exclude unbounded durations. Duration `distinctBy` is ignored,
as in the existing backend SQL implementation. Physical date-range columns require dialect support for
extracting half-open bounds through `CompilerDialect.dateRangeColumn`.

The backend's sum, count, quarter-count, duration-sum, and flag-output adapters delegate to the shared converters.
Flag filters compile separately as event predicates.

## Output select extraction

`ResolvedSelectConverter` handles aggregation, first/last/random/distinct values, date union/distance, flags,
event-date union/duration, exists, and concept values. Callers provide the existing generated alias, table graph, IDs,
optional validity date, resolved concept-column table mapping, and ordered prepared concept-value sources through
`SelectConversionContext`.

| Original backend responsibility | Shared implementation | Backend responsibility retained |
| --- | --- | --- |
| `ValueSelectUtil` | `compiler.conversion.operation.ValueSelectUtil` | First/last select DTO adaptation |
| `RandomValueSelectConverter` SQL | Shared converter with the same name | Column resolution and alias allocation |
| `DistinctSelectConverter` SQL | Shared converter with the same name | Select DTO adaptation |
| `ClickhouseDistinctSelectConverter` SQL | Shared converter with the same name | Dialect selection |
| `MappableSingleColumnSelect.getSubstringSelect` SQL | `SubstringSelect.getSubstringSelect` | Mapping, result readers, range adaptation |
| `DaterangeSelectUtil` select SQL | Shared helper with the same name | Resolved date-column adaptation |
| `DateDistanceSqlAggregator` select SQL | Shared helper with the same name | Frozen/per-row end-date resolution and filter/export SQL |
| `FlagSqlAggregator` select SQL | Shared aggregation converter | Filter/export predicates |
| Event date/duration select SQL | Shared converters with the original names | Application select metadata |
| `ConceptColumnSelectConverter` SQL | Shared converter with the same name | Connector-column resolution and mapping-table preparation |

First and last preserve the existing validity-date ordering and entity-ID fallback, without adding tie breakers.
Random selects continue to ignore substring bounds. Distinct selects retain separate HANA and ClickHouse algorithms,
including their existing ordering and empty/null handling. Substring bounds remain zero-based with an exclusive end.
Concept-ID mapping tables are currently prepared by the backend and passed to the connector as ordered SQL-resolved
sources. Their preparation belongs with the connector once the reusable table-preparation lifecycle is extracted.

## Validation

The resolved model uses Jakarta Bean Validation annotations. The application supplies a validation provider, normally
Hibernate Validator, and calls `ResolvedQueryValidation` once before passing a query to the SQL compiler. Validation is
cascaded through the complete query graph, including extension-provided filters, selects, conditions, and aggregations.

Model records may reference declarative constraints from `validation`, but validation services must never enrich or
rewrite the model. Conversion and execution code consumes a successfully validated `ResolvedQuery` and must not perform
logical identifier resolution.

Compact record constructors only create immutable copies of collections. Domain constraints such as compatible column
types, ordered ranges, and same-table requirements are declarative and are reported together as constraint violations.
