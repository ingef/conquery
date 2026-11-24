/**
 * Framework-neutral SQL connector contracts and services.
 *
 * <p>{@code model} defines the fully resolved input, {@code validation} verifies that input, and {@code translate}
 * defines framework-neutral compilation contracts. Conversion and execution implementations may depend on these
 * contracts; model code must not depend on translate or execution services.</p>
 */
package com.bakdata.conquery.sql;
