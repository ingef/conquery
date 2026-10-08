package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.compiler.CompiledQuery;
import com.bakdata.conquery.sql.compiler.FormSqlCompiler;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

/** Backend boundary for resolved form compilation. */
public final class ResolvedFormAdapter {

	private final ResolvedFormMapper mapper;
	private final FormSqlCompiler compiler;

	public ResolvedFormAdapter(ResolvedFormMapper mapper, FormSqlCompiler compiler) {
		this.mapper = mapper;
		this.compiler = compiler;
	}

	public SqlQuery compile(AbsoluteFormQuery query, CompilerDialect dialect) {
		return adapt(compiler.compile(mapper.map(query), dialect), query);
	}

	public SqlQuery compile(RelativeFormQuery query, CompilerDialect dialect) {
		return adapt(compiler.compile(mapper.map(query), dialect), query);
	}

	public SqlQuery compile(EntityDateQuery query, CompilerDialect dialect) {
		return adapt(compiler.compile(mapper.map(query), dialect), query);
	}

	private static SqlQuery adapt(CompiledQuery compiled, com.bakdata.conquery.apiv1.query.Query query) {
		return SqlQuery.fromCompiled(compiled, query.getResultInfos());
	}
}
