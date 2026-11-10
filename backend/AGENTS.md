# Java Code Guidelines

* Treat serialized JSON, configuration, and API property/type names as compatibility-sensitive; do not rename or remove them without checking existing payloads and migrations.
* Prefer records for immutable data carriers; otherwise follow the surrounding Lombok style.
* Don't use `var`.
* Declare local variables final, unless reassignment is necessary.
* Don't run tests unless explicitly instructed. 
* Be specific when selecting tests to run.
* Avoid writing overly specific tests. Prefer testing functionality and behavior.
* Prefer established patterns and utilities in the affected package over introducing new abstractions or dependencies.

# Toolchain

* Compile with `& $mvn -pl backend -DskipTests compile --quiet`.
* Check Java formatting with `& $mvn -pl backend spotless:check`. Only before committing.

# Behaviour

* When committing, always add a `Co-Authored-By` trailer for the active AI model.


