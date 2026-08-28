package com.bakdata.conquery.sql.compiler.dialect;

/**
 * Database-specific capabilities exposed to the framework-neutral SQL compiler.
 *
 * <p>This contract belongs to the SQL connector and must only use types owned by the connector or its public model
 * dependencies. Backend query DTOs, converter registries, runtime connections, and result processing are deliberately
 * excluded.</p>
 *
 * <p>Concrete backend integrations may extend this contract with temporary adapter interfaces while the legacy compiler
 * is migrated. Those adapters must not become dependencies of the framework-neutral compiler.</p>
 */
public interface CompilerDialect {

	/** Maximum identifier length supported by the target database. */
	int getNameMaxLength();

	/** Whether the dialect represents date ranges in one database column instead of separate start and end columns. */
	default boolean supportsSingleColumnRanges() {
		return false;
	}
}
