package com.bakdata.conquery.sql.conversion;

import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import com.bakdata.conquery.sql.translate.FormSqlTranslator;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.validation.ResolvedModelValidation;

/** Backend boundary for resolved form compilation. */
public final class ResolvedFormAdapter {

	private final ResolvedFormMapper mapper;
	private final FormSqlTranslator translator;
	private final ResolvedModelValidation validation;

	public ResolvedFormAdapter(Validator validator, ResolvedFormMapper mapper, FormSqlTranslator translator) {
		this.validation = new ResolvedModelValidation(validator);
		this.mapper = mapper;
		this.translator = translator;
	}

	public SqlQuery translate(AbsoluteFormQuery query, TranslatorDialect dialect) {
		return translate(mapper.map(query), query, dialect);
	}

	public SqlQuery translate(RelativeFormQuery query, TranslatorDialect dialect) {
		return translate(mapper.map(query), query, dialect);
	}

	public SqlQuery translate(EntityDateQuery query, TranslatorDialect dialect) {
		return translate(mapper.map(query), query, dialect);
	}

	private SqlQuery translate(ResolvedFormQuery resolved, Query query, TranslatorDialect dialect) {
		validation.validate(resolved);
		return adapt(translator.translate(resolved, dialect), query);
	}

	private static SqlQuery adapt(TranslatedQuery compiled, Query query) {
		return SqlQuery.fromCompiled(compiled, query.getResultInfos());
	}
}
