package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import com.bakdata.conquery.sql.translate.FormSqlTranslator;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

/** Backend boundary for resolved form compilation. */
public final class ResolvedFormAdapter {

	private final ResolvedFormMapper mapper;
	private final FormSqlTranslator translator;

	public ResolvedFormAdapter(ResolvedFormMapper mapper, FormSqlTranslator translator) {
		this.mapper = mapper;
		this.translator = translator;
	}

	public SqlQuery translate(AbsoluteFormQuery query, TranslatorDialect dialect) {
		return adapt(translator.translate(mapper.map(query), dialect), query);
	}

	public SqlQuery translate(RelativeFormQuery query, TranslatorDialect dialect) {
		return adapt(translator.translate(mapper.map(query), dialect), query);
	}

	public SqlQuery translate(EntityDateQuery query, TranslatorDialect dialect) {
		return adapt(translator.translate(mapper.map(query), dialect), query);
	}

	private static SqlQuery adapt(TranslatedQuery compiled, com.bakdata.conquery.apiv1.query.Query query) {
		return SqlQuery.fromCompiled(compiled, query.getResultInfos());
	}
}
