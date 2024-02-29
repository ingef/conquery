package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Field;

class PreprocessingCte extends ConnectorCte {

	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> forPreprocessing = tableContext.allSqlSelects().stream()
													   .flatMap(sqlSelects -> sqlSelects.getPreprocessingSelects().stream())
													   .toList();

		// we alias the primary column, so we can rely upon in other places that it has a specific name
		Field<Object> aliasesPrimaryColumn = tableContext.getPrimaryColumn().as(SharedAliases.PRIMARY_COLUMN.getAlias());

		Selects preprocessingSelects = Selects.builder()
											  .primaryColumn(aliasesPrimaryColumn)
											  .validityDate(tableContext.getValidityDate())
											  .sqlSelects(forPreprocessing)
											  .build();

		// all where clauses that don't require any preprocessing (connector/child conditions)
		List<Condition> conditions = tableContext.getSqlFilters().stream()
												 .flatMap(sqlFilter -> sqlFilter.getWhereClauses().getPreprocessingConditions().stream())
												 .map(WhereCondition::condition)
												 .toList();

		return QueryStep.builder()
						.selects(preprocessingSelects)
						.conditions(conditions)
						.fromTable(QueryStep.toTableLike(tableContext.getConnectorTables().getPredecessor(ConnectorCteStep.PREPROCESSING)));
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.PREPROCESSING;
	}

}
