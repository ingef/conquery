package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.sql.compiler.CompiledQuery;
import com.bakdata.conquery.sql.compiler.TableExportSqlCompiler;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

/** Backend boundary for resolved row-level table exports. */
public final class ResolvedTableExportAdapter {

	private final ResolvedTableExportMapper mapper;
	private final TableExportSqlCompiler compiler;

	public ResolvedTableExportAdapter(ResolvedTableExportMapper mapper, TableExportSqlCompiler compiler) {
		this.mapper = mapper;
		this.compiler = compiler;
	}

	public SqlQuery compile(TableExportQuery query, CompilerDialect dialect) {
		CompiledQuery compiled = compiler.compile(mapper.map(query, dialect), dialect);
		return SqlQuery.fromCompiled(compiled, query.getResultInfos());
	}
}
