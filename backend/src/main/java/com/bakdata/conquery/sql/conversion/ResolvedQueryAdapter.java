package com.bakdata.conquery.sql.conversion;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.compiler.ColumnRole;
import com.bakdata.conquery.sql.compiler.CompiledQuery;
import com.bakdata.conquery.sql.compiler.SqlCompiler;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.validation.ResolvedQueryValidation;
import jakarta.validation.Validator;

/** Validates resolved application input and adapts connector output to the existing backend query contract. */
public final class ResolvedQueryAdapter {

	private final ResolvedQueryValidation validation;
	private final SqlCompiler compiler;

	public ResolvedQueryAdapter(Validator validator, SqlCompiler compiler) {
		this.validation = new ResolvedQueryValidation(validator);
		this.compiler = compiler;
	}

	public SqlQuery compile(
			ResolvedQuery resolvedQuery,
			CompilerDialect dialect,
			List<ResultInfo> resultInfos
	) {
		validation.validate(resolvedQuery);
		CompiledQuery compiled = compiler.compile(resolvedQuery, dialect);
		long resultColumnCount = compiled.columns().stream()
				.filter(column -> column.role() == ColumnRole.RESULT)
				.count();
		if (resultColumnCount != resultInfos.size()) {
			throw new IllegalStateException(
					"Compiler returned %d result columns for %d backend result descriptors"
							.formatted(resultColumnCount, resultInfos.size())
			);
		}
		return SqlQuery.fromCompiled(compiled, resultInfos);
	}
}
