package com.bakdata.conquery.sql.compiler.ir;

import java.util.List;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import lombok.experimental.UtilityClass;

/** Composes compiled child steps for a logical conjunction or disjunction. */
@UtilityClass
public final class LogicalQueryStepCompiler {

	public static QueryStep compile(
			List<QueryStep> childSteps,
			JoinMode joinMode,
			DateAggregationAction dateAggregationAction,
			boolean createExists,
			EntitySchema entitySchema,
			CompilerDialect compilerDialect,
			SqlNameGenerator nameGenerator
	) {
		QueryStep joined = QueryStepComposer.joinSteps(
				childSteps,
				joinMode,
				dateAggregationAction,
				entitySchema,
				compilerDialect,
				nameGenerator
		);
		if (!createExists) {
			return joined;
		}
		return joined.addSqlSelect(ExistsSqlSelect.withAlias(joined.getCteName()));
	}
}
