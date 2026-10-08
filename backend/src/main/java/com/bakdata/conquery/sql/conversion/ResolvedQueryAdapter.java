package com.bakdata.conquery.sql.conversion;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.compiler.ColumnRole;
import com.bakdata.conquery.sql.compiler.CompiledQuery;
import com.bakdata.conquery.sql.compiler.SqlCompiler;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.validation.ResolvedQueryValidation;

/** Maps initialized backend queries, validates the resolved input, and adapts compiler output to the backend contract. */
public final class ResolvedQueryAdapter {

	private final ResolvedQueryValidation validation;
	private final SqlCompiler compiler;
	private final ResolvedQueryMapper mapper;

	public ResolvedQueryAdapter(Validator validator, SqlCompiler compiler, ResolvedQueryMapper mapper) {
		this.validation = new ResolvedQueryValidation(validator);
		this.compiler = compiler;
		this.mapper = mapper;
	}

	public SqlQuery compile(ConceptQuery query, CompilerDialect dialect) {
		return compile(query, Optional.empty(), query.getResultInfos(), dialect);
	}

	public SqlQuery compile(SecondaryIdQuery query, CompilerDialect dialect) {
		return compile(
				query.getQuery(),
				Optional.of(query.getSecondaryId().resolve()),
				query.getResultInfos(),
				dialect
		);
	}

	private SqlQuery compile(
			ConceptQuery query,
			Optional<SecondaryIdDescription> secondaryId,
			List<ResultInfo> resultInfos,
			CompilerDialect dialect
	) {
		return compile(mapper.map(query, secondaryId, resultInfos), dialect, resultInfos);
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
