package com.bakdata.conquery.sql.conversion;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.translate.ColumnRole;
import com.bakdata.conquery.sql.translate.SqlTranslator;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.validation.ResolvedModelValidation;

/** Maps initialized backend queries, validates the resolved input, and adapts translate output to the backend contract. */
public final class ResolvedQueryAdapter {

	private final ResolvedModelValidation validation;
	private final SqlTranslator translator;
	private final ResolvedQueryMapper mapper;

	public ResolvedQueryAdapter(Validator validator, SqlTranslator translator, ResolvedQueryMapper mapper) {
		this.validation = new ResolvedModelValidation(validator);
		this.translator = translator;
		this.mapper = mapper;
	}

	public SqlQuery translate(ConceptQuery query, TranslatorDialect dialect) {
		return translate(query, Optional.empty(), query.getResultInfos(), dialect);
	}

	public SqlQuery translate(SecondaryIdQuery query, TranslatorDialect dialect) {
		return translate(
				query.getQuery(),
				Optional.of(query.getSecondaryId().resolve()),
				query.getResultInfos(),
				dialect
		);
	}

	private SqlQuery translate(
			ConceptQuery query,
			Optional<SecondaryIdDescription> secondaryId,
			List<ResultInfo> resultInfos,
			TranslatorDialect dialect
	) {
		return translate(mapper.map(query, secondaryId, resultInfos), dialect, resultInfos);
	}

	public SqlQuery translate(
			ResolvedQuery resolvedQuery,
			TranslatorDialect dialect,
			List<ResultInfo> resultInfos
	) {
		validation.validate(resolvedQuery);
		TranslatedQuery compiled = translator.translate(resolvedQuery, dialect);
		long resultColumnCount = compiled.columns().stream()
				.filter(column -> column.role() == ColumnRole.RESULT)
				.count();
		if (resultColumnCount != resultInfos.size()) {
			throw new IllegalStateException(
					"Translator returned %d result columns for %d backend result descriptors"
							.formatted(resultColumnCount, resultInfos.size())
			);
		}
		return SqlQuery.fromCompiled(compiled, resultInfos);
	}
}
