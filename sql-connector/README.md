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

The model is organized by responsibility:

- `query.node` contains the normalized query tree;
- `query.operation` contains the open operation interfaces and standard implementations;
- `query.schema` contains resolved physical tables, columns, connectors, and entity metadata;
- `query.range` and `query.result` contain shared value objects;
- `query.validation` contains the validation boundary and reusable constraints.

Shared dataset vocabulary such as `ColumnType` comes from the dependency-free `dataset-model` module. SQL-specific
physical metadata remains in `query.schema`.

`ResolvedFilter`, `ResolvedSelect`, `ResolvedCondition`, and `ResolvedAggregation` are open extension points.
Implementations must be immutable and carry resolved columns and typed values. They must not carry unresolved repository
identifiers or arbitrary SQL received from a query request.

The `BuiltInFilters`, `BuiltInSelects`, `BuiltInConditions`, and `BuiltInAggregations` types define the normalized
operations understood by the standard compiler. They intentionally describe semantics rather than the configuration
classes used by a particular query-producing application.

## Validation

The resolved model uses Jakarta Bean Validation annotations. The application supplies a validation provider, normally
Hibernate Validator, and calls `ResolvedQueryValidation` once before passing a query to the SQL compiler. Validation is
cascaded through the complete query graph, including extension-provided filters, selects, conditions, and aggregations.

Compact record constructors only create immutable copies of collections. Domain constraints such as compatible column
types, ordered ranges, and same-table requirements are declarative and are reported together as constraint violations.
