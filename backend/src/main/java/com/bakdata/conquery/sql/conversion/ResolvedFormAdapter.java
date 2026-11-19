package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import com.bakdata.conquery.sql.translate.FormSqlTranslator;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import lombok.RequiredArgsConstructor;

/** Backend boundary for resolved form compilation. */
@RequiredArgsConstructor
public final class ResolvedFormAdapter {

	private final ResolvedQuerySourceMapper mapper;
	private final FormSqlTranslator translator;

	public SqlQuery translate(AbsoluteFormQuery query, TranslatorDialect dialect) {
		return translate(mapper.mapForm(query), query, dialect);
	}

	public SqlQuery translate(RelativeFormQuery query, TranslatorDialect dialect) {
		return translate(mapper.mapForm(query), query, dialect);
	}

	public SqlQuery translate(EntityDateQuery query, TranslatorDialect dialect) {
		return translate(mapper.mapForm(query), query, dialect);
	}

	private SqlQuery translate(
			ResolvedFormQuery resolved,
			com.bakdata.conquery.apiv1.query.Query query,
			TranslatorDialect dialect
	) {
		final TranslatedQuery compiled = translator.translate(resolved, dialect);
		return SqlQuery.fromCompiled(compiled, resolved.resultColumns(), query.getResultInfos());
	}
}
