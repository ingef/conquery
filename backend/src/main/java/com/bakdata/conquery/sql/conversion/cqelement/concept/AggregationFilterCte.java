package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Condition;

class AggregationFilterCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		String aggregationFilterPredecessorCte = cteContext.getConceptTables().getPredecessorTableName(CteStep.AGGREGATION_FILTER);

		final Optional<ColumnDateRange> validityDate;
		if (cteContext.isExcludedFromDateAggregation()) {
			validityDate = Optional.empty();
		}
		else {
			validityDate = cteContext.getValidityDateRange().map(_validityDate -> _validityDate.qualify(aggregationFilterPredecessorCte));
		}

		ConceptSelects aggregationFilterSelect = new ConceptSelects(
				cteContext.getPrimaryColumn(),
				validityDate,
				getAggregationFilterSelects(cteContext, aggregationFilterPredecessorCte)
		);
		List<Condition> aggregationFilterConditions = cteContext.getFilters().stream()
																.flatMap(conceptFilter -> conceptFilter.getFilters().getGroup().stream())
																.map(FilterCondition::filterCondition)
																.toList();

		return QueryStep.builder()
						.selects(aggregationFilterSelect)
						.conditions(aggregationFilterConditions);
	}

	private List<SqlSelect> getAggregationFilterSelects(CteContext cteContext, String aggregationFilterPredecessorCte) {
		return cteContext.getSelects().stream()
						 .flatMap(sqlSelects -> sqlSelects.getForFinalStep().stream())
						 // TODO: EXISTS edge case is only in a concepts final select statement and has no predecessor selects
						 .filter(conquerySelect -> !(conquerySelect instanceof ExistsSqlSelect))
						 .map(conquerySelect -> ExtractingSqlSelect.fromConquerySelect(conquerySelect, aggregationFilterPredecessorCte))
						 .distinct()
						 .collect(Collectors.toList());
	}

	@Override
	public CteStep cteStep() {
		return CteStep.AGGREGATION_FILTER;
	}

}
