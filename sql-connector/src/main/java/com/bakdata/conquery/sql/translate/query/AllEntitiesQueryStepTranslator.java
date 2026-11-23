package com.bakdata.conquery.sql.translate.query;

import java.util.Optional;

import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Compiles the resolved entity schema into the query step used to select every entity. */
@UtilityClass
public final class AllEntitiesQueryStepTranslator {

	public static QueryStep translate(
			@NonNull AllEntitiesNode allEntitiesNode,
			EntitySchema entitySchema,
			TranslatorDialect translatorDialect,
			String cteName
	) {
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(SchemaSql.field(entitySchema.primaryId(), String.class)))
				.validityDate(Optional.of(
						translatorDialect.emptyDateRange().asValidityDateRange(cteName)
				))
				.build();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(SchemaSql.table(entitySchema.table()))
				.build();
	}
}
