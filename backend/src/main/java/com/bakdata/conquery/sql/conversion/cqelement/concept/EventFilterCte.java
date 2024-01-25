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

class EventFilterCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {
		Selects eventFilterSelects = getEventFilterSelects(conceptCteContext);
		List<Condition> eventFilterConditions = conceptCteContext.getFilters().stream()
																 .flatMap(conceptFilter -> conceptFilter.getWhereClauses().getEventFilters().stream())
																 .map(WhereCondition::condition)
																 .toList();
		return QueryStep.builder()
						.selects(eventFilterSelects)
						.conditions(eventFilterConditions);
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.EVENT_FILTER;
	}

	private Selects getEventFilterSelects(ConceptCteContext conceptCteContext) {
		String predecessorTableName = conceptCteContext.getConceptTables().getPredecessor(cteStep());

		Field<Object> primaryColumn = QualifyingUtil.qualify(conceptCteContext.getPrimaryColumn(), predecessorTableName);

		Optional<ColumnDateRange> validityDate = conceptCteContext.getValidityDate();
		if (validityDate.isPresent()) {
			validityDate = Optional.of(validityDate.get().qualify(predecessorTableName));
		}

		List<? extends SqlSelect> sqlSelects = conceptCteContext.allConceptSelects()
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
