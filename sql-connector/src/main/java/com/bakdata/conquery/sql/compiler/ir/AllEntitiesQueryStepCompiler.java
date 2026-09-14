package com.bakdata.conquery.sql.compiler.ir;

import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import lombok.experimental.UtilityClass;

/** Compiles the resolved entity schema into the query step used to select every entity. */
@UtilityClass
public final class AllEntitiesQueryStepCompiler {

	private static final String ALL_ENTITIES_CTE = "all_ids";

	public static QueryStep compile(
			AllEntitiesNode allEntitiesNode,
			EntitySchema entitySchema,
			CompilerDialect compilerDialect
	) {
		Objects.requireNonNull(allEntitiesNode, "allEntitiesNode");
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(EntitySchemaSql.primaryId(entitySchema)))
				.validityDate(Optional.of(
						compilerDialect.emptyDateRange().asValidityDateRange(ALL_ENTITIES_CTE)
				))
				.build();
		return QueryStep.builder()
				.cteName(ALL_ENTITIES_CTE)
				.selects(selects)
				.fromTable(EntitySchemaSql.table(entitySchema))
				.build();
	}
}
