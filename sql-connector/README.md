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

`ResolvedFilter`, `ResolvedSelect`, and `ResolvedCondition` are open extension points. Implementations must be immutable
and carry resolved columns and typed values. They must not carry unresolved repository identifiers or arbitrary SQL
received from a query request.
