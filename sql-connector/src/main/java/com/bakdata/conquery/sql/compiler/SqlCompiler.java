package com.bakdata.conquery.sql.compiler;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.model.ResolvedQuery;

/**
 * Compiles a fully resolved query into self-contained SQL for a selected database dialect.
 *
 * <p>The caller owns dataset and datasource selection, query execution, and result post-processing. Implementations
 * must compile exclusively from the supplied query and dialect and must not resolve identifiers or access backend
 * repositories.</p>
 */
@FunctionalInterface
public interface SqlCompiler {

	CompiledQuery compile(ResolvedQuery query, CompilerDialect dialect);
}
