package com.bakdata.conquery.sql.translate.query;

import java.util.List;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import lombok.experimental.UtilityClass;

/** Composes compiled child steps for a logical conjunction or disjunction. */
@UtilityClass
public final class LogicalQueryStepTranslator {

	public static QueryStep translate(
			List<QueryStep> childSteps,
			JoinMode joinMode,
			DateAggregationAction dateAggregationAction,
			boolean createExists,
			EntitySchema entitySchema,
			TranslatorDialect translatorDialect,
			SqlNameGenerator nameGenerator
	) {
		QueryStep joined = QueryStepComposer.joinSteps(
				childSteps,
				joinMode,
				dateAggregationAction,
				entitySchema,
				translatorDialect,
				nameGenerator
		);
		if (!createExists) {
			return joined;
		}
		return joined.addSqlSelect(ExistsSqlSelect.withAlias(joined.getCteName()));
	}
}
