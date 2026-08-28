/**
 * Framework-neutral, fully resolved input model for SQL query compilation.
 *
 * <p>Instances in this package must not require repository access or identifier resolution during compilation. They
 * describe query semantics and physical database objects, but deliberately exclude connections, dialects, and SQL
 * compiler state.</p>
 *
 * <p>The root query aggregate is complemented by dedicated packages for query nodes, operations, ranges, result
 * metadata, and physical schema references. Validation and SQL business logic live in sibling packages so this package
 * remains the connector's immutable input boundary.</p>
 */
package com.bakdata.conquery.sql.model;
