package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.translate.TableExportSqlTranslator;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
/** Backend boundary for resolved row-level table exports. */
public record ResolvedTableExportAdapter(
		ResolvedQuerySourceMapper mapper,
		TableExportSqlTranslator translator
) {

	public SqlQuery translate(TableExportQuery query, TranslatorDialect dialect) {
		final ResolvedTableExportQuery resolved = mapper.mapTableExport(query);
		final TranslatedQuery compiled = translator.translate(resolved, dialect);
		return SqlQuery.fromCompiled(compiled, resolved.resultColumns(), query.getResultInfos());
	}
}
