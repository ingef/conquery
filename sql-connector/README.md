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

Flag aggregation for output selects and integration of resolved operations into the legacy backend remain separate
migration steps. Flag filters already compile as event predicates.

## Validation

The resolved model uses Jakarta Bean Validation annotations. The application supplies a validation provider, normally
Hibernate Validator, and calls `ResolvedQueryValidation` once before passing a query to the SQL compiler. Validation is
cascaded through the complete query graph, including extension-provided filters, selects, conditions, and aggregations.

Model records may reference declarative constraints from `validation`, but validation services must never enrich or
rewrite the model. Conversion and execution code consumes a successfully validated `ResolvedQuery` and must not perform
logical identifier resolution.

Compact record constructors only create immutable copies of collections. Domain constraints such as compatible column
types, ordered ranges, and same-table requirements are declarative and are reported together as constraint violations.
