package com.bakdata.conquery.sql.translate.query;

import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import lombok.experimental.UtilityClass;

/** Compiles the resolved entity schema into the query step used to select every entity. */
@UtilityClass
public final class AllEntitiesQueryStepTranslator {

	private static final String ALL_ENTITIES_CTE = "all_ids";

	public static QueryStep translate(
			AllEntitiesNode allEntitiesNode,
			EntitySchema entitySchema,
			TranslatorDialect translatorDialect
	) {
		Objects.requireNonNull(allEntitiesNode, "allEntitiesNode");
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(SchemaSql.field(entitySchema.primaryId(), String.class)))
				.validityDate(Optional.of(
						translatorDialect.emptyDateRange().asValidityDateRange(ALL_ENTITIES_CTE)
				))
				.build();
		return QueryStep.builder()
				.cteName(ALL_ENTITIES_CTE)
				.selects(selects)
				.fromTable(SchemaSql.table(entitySchema.table()))
				.build();
	}
}
