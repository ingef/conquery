package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.translate.TableExportSqlTranslator;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import lombok.RequiredArgsConstructor;

/** Backend boundary for resolved row-level table exports. */
@RequiredArgsConstructor
public final class ResolvedTableExportAdapter {

	private final ResolvedQuerySourceMapper mapper;
	private final TableExportSqlTranslator translator;

	public SqlQuery translate(TableExportQuery query, TranslatorDialect dialect) {
		TranslatedQuery compiled = translator.translate(mapper.mapTableExport(query), dialect);
		return SqlQuery.fromCompiled(compiled, query.getResultInfos());
	}
}
