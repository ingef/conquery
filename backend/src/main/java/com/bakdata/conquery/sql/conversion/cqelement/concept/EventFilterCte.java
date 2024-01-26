package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class EventFilterCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext CQTableContext) {
		Selects eventFilterSelects = getEventFilterSelects(CQTableContext);
		List<Condition> eventFilterConditions = CQTableContext.getFilters().stream()
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

	private Selects getEventFilterSelects(CQTableContext CQTableContext) {
		String predecessorTableName = CQTableContext.getConceptTables().getPredecessor(cteStep());

		Optional<ColumnDateRange> validityDate = CQTableContext.getValidityDate();
		if (validityDate.isPresent()) {
			validityDate = Optional.of(validityDate.get().qualify(predecessorTableName));
		}

		List<? extends SqlSelect> sqlSelects = CQTableContext.allConceptSelects()
															 .flatMap(selects -> selects.getAggregationSelects().stream())
															 .map(sqlSelect -> sqlSelect.createColumnReference(predecessorTableName))
															 .toList();

		return Selects.builder()
					  .primaryColumn(CQTableContext.getPrimaryColumn())
					  .validityDate(validityDate)
					  .sqlSelects(sqlSelects)
					  .build();
	}

}
