/**
 * Framework-neutral, fully resolved input model for SQL query compilation.
 *
 * <p>Instances in this package must not require repository access or identifier resolution during compilation. They
 * describe query semantics and physical database objects, but deliberately exclude connections, dialects, and SQL
 * compiler state.</p>
 */
package com.bakdata.conquery.sql.query;
