# Java Code Guidelines

* Prefer established patterns and utilities in the affected package over introducing new abstractions or dependencies.
* Treat serialized JSON, configuration, and API property/type names as compatibility-sensitive; do not rename or remove them without checking existing payloads and migrations.
* Prefer records for immutable data carriers; otherwise follow the surrounding Lombok style.
* Don't use `var`.
* Declare local variables final, unless reassignment is necessary.
* When committing, always add a `Co-Authored-By` trailer for the active AI model.
  
## Testing

- Run the narrowest relevant tests.
- Test observable behavior rather than implementation details or framework metadata. A test should remain valuable after a behavior-preserving refactor.
- Prioritize functional and business behavior, API and persistence contracts, validation, error handling, security, and meaningful component integration.

# Toolchain

* Compile with `& $mvn -pl backend -DskipTests compile --quiet`.
* Check Java formatting with `& $mvn -pl backend spotless:check`. Only before committing.
