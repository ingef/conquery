package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Condition;

class AggregationFilterCte extends ConceptCte {

	@Override
	public boolean canConvert(CteContext cteContext) {
		return true;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		String aggregationSelectCteName = cteContext.getConceptTableNames().tableNameFor(CteStep.AGGREGATION_SELECT);
		List<ConquerySelect> aggregationFilterSelects = cteContext.getSelects().stream()
																  .flatMap(sqlSelects -> sqlSelects.getForFinalStep().stream())
																  .toList();

		final Optional<ColumnDateRange> validityDate;
		if (cteContext.isExcludedFromDateAggregation()) {
			validityDate = Optional.empty();
		}
		else {
			validityDate = cteContext.getValidityDateRange().map(_validityDate -> _validityDate.qualify(aggregationSelectCteName));
		}

		List<Condition> aggregationFilterConditions = cteContext.getFilters().stream()
														  .flatMap(conceptFilter -> conceptFilter.getFilters().getGroup().stream())
														  .map(FilterCondition::filterCondition)
														  .toList();

		return QueryStep.builder()
						.selects(new ConceptSelects(cteContext.getPrimaryColumn(), validityDate, aggregationFilterSelects))
						.conditions(aggregationFilterConditions);
	}

	@Override
	public CteStep cteStep() {
		return CteStep.AGGREGATION_FILTER;
	}

}
