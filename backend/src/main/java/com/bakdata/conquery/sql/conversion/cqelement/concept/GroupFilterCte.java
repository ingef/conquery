package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Condition;

class GroupFilterCte extends ConceptCte {

	@Override
	public boolean canConvert(CteContext cteContext) {
		return true;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		String groupSelectCteName = cteContext.getConceptTableNames().tableNameFor(CteStep.GROUP_SELECT);
		List<ConquerySelect> groupFilterSelects = cteContext.getSelects().stream()
															.flatMap(sqlSelects -> sqlSelects.getForGroupFilterStep().stream())
															.toList();

		final Optional<ColumnDateRange> validityDate;
		if (cteContext.isExcludedFromDateAggregation()) {
			validityDate = Optional.empty();
		}
		else {
			validityDate = cteContext.getValidityDateRange().map(_validityDate -> _validityDate.qualify(groupSelectCteName));
		}

		List<Condition> groupFilterConditions = cteContext.getFilters().stream()
														  .flatMap(conceptFilter -> conceptFilter.getFilters().getGroup().stream())
														  .map(FilterCondition::filterCondition)
														  .toList();

		return QueryStep.builder()
						.selects(new ConceptSelects(cteContext.getPrimaryColumn(), validityDate, groupFilterSelects))
						.conditions(groupFilterConditions);
	}

	@Override
	public CteStep cteStep() {
		return CteStep.GROUP_FILTER;
	}

}
