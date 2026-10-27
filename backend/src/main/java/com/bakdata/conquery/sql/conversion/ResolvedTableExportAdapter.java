package com.bakdata.conquery.sql.conversion;

import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.translate.TableExportSqlTranslator;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.validation.ResolvedModelValidation;

/** Backend boundary for resolved row-level table exports. */
public final class ResolvedTableExportAdapter {

	private final ResolvedTableExportMapper mapper;
	private final TableExportSqlTranslator translator;
	private final ResolvedModelValidation validation;

	public ResolvedTableExportAdapter(
			Validator validator,
			ResolvedTableExportMapper mapper,
			TableExportSqlTranslator translator
	) {
		this.validation = new ResolvedModelValidation(validator);
		this.mapper = mapper;
		this.translator = translator;
	}

	public SqlQuery translate(TableExportQuery query, TranslatorDialect dialect) {
		ResolvedTableExportQuery resolved = mapper.map(query, dialect);
		validation.validate(resolved);
		TranslatedQuery compiled = translator.translate(resolved, dialect);
		return SqlQuery.fromCompiled(compiled, query.getResultInfos());
	}
}
