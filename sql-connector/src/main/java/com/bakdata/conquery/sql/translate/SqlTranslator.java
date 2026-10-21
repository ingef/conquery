package com.bakdata.conquery.sql.translate;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;

/**
 * Compiles a fully resolved query into self-contained SQL for a selected database dialect.
 *
 * <p>The caller owns dataset and datasource selection, query execution, and result post-processing. Implementations
 * must compile exclusively from the supplied query and dialect and must not resolve identifiers or access backend
 * repositories.</p>
 */
@FunctionalInterface
public interface SqlTranslator {

	TranslatedQuery translate(ResolvedQuery query, TranslatorDialect dialect);
}
