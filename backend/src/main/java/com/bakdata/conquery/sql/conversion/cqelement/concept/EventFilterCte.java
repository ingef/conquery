package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Field;

class EventFilterCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {
		Selects eventFilterSelects = getEventFilterSelects(tableContext);
		List<Condition> eventFilterConditions = tableContext.getSqlFilters().stream()
															.flatMap(conceptFilter -> conceptFilter.getWhereClauses().getEventFilters().stream())
															.map(WhereCondition::condition)
															.toList();
		return QueryStep.builder()
						.selects(eventFilterSelects)
						.conditions(eventFilterConditions);
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.EVENT_FILTER;
	}

	private Selects getEventFilterSelects(CQTableContext tableContext) {
		String predecessorTableName = tableContext.getConnectorTables().getPredecessor(cteStep());

		Field<Object> primaryColumn = QualifyingUtil.qualify(tableContext.getPrimaryColumn(), predecessorTableName);

		Optional<ColumnDateRange> validityDate = tableContext.getValidityDate();
		if (validityDate.isPresent()) {
			validityDate = Optional.of(validityDate.get().qualify(predecessorTableName));
		}

		List<? extends SqlSelect> sqlSelects = tableContext.allSqlSelects().stream()
														   .flatMap(selects -> selects.getAggregationSelects().stream())
														   .map(sqlSelect -> sqlSelect.createColumnReference(predecessorTableName))
														   .toList();

		return Selects.builder()
					  .primaryColumn(primaryColumn)
					  .validityDate(validityDate)
					  .sqlSelects(sqlSelects)
					  .build();
	}

}
