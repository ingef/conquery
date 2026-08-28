/**
 * Framework-neutral SQL connector contracts and services.
 *
 * <p>{@code model} defines the fully resolved input, {@code validation} verifies that input, and sibling packages such
 * as {@code conversion}, {@code dialect}, and {@code execution} contain SQL business logic. Business logic may depend on
 * the model and validation boundary; model code must not depend on compiler or execution services.</p>
 */
package com.bakdata.conquery.sql;
