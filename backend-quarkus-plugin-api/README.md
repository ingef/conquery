# Conquery Quarkus Plugin API

This artifact is the compilation contract for third-party Conquery model plugins. A plugin should depend on
`backend-quarkus-plugin-api` with Maven scope `provided`; it must not depend on the `backend-quarkus` application.

The current stable surface covers metadata filters:

- `FilterDefinition`, `AbstractFilterDefinition`, and `SingleColumnFilterDefinition` describe plugin metadata.
- `FilterDefinitionProvider<T>` is the CDI discovery hook.
- `FilterConversionContext` validates referenced columns without exposing backend repositories or IDs. It returns the
  shared dataset `ColumnDescriptor` and `ColumnType` contracts rather than filter-specific column models.
- `FilterResult` is the implementation-neutral result mapped to the backend catalog by the host.
- `PolymorphicModelSubtype` declares the JSON/OpenAPI discriminator.

Use frontend filter-value discriminator IDs such as `STRING` in `acceptedValueTypes()`. Include a Jandex index in the
plugin JAR. The complete development, installation, reaugmentation, and test workflow is documented in
[`backend-quarkus/README.md`](../backend-quarkus/README.md#polymorphic-model-plugins).

Query filter-value, select, and condition contracts have not yet been extracted into this artifact and should be
considered provisional.
