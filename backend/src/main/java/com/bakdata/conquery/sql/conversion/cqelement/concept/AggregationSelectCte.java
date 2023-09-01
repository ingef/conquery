package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.models.ColumnDateRange;

class AggregationSelectCte extends ConceptCte {

	@Override
	public boolean canConvert(CteContext cteContext) {
		// We always need to apply a group by
		return true;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		// all selects that are required in the aggregation filter step
		String previousCteName = cteContext.getPrevious().getCteName();
		List<ConquerySelect> aggregationFilterSelects = cteContext.allConceptSelects()
																  .flatMap(sqlSelects -> sqlSelects.getForAggregationSelectStep().stream())
																  // as we don't know if there is an event filter step, we just map the selects on the previous step
																  .map(conquerySelect -> conquerySelect.qualify(previousCteName))
																  .distinct()
																  .collect(Collectors.toList());

		Optional<ColumnDateRange> aggregatedValidityDate = cteContext.getValidityDateRange()
																	 .map(validityDate -> validityDate.qualify(previousCteName)
																									  .aggregated()
																									  .asValidityDateRange(cteContext.getConceptLabel())
																	 );

		ConceptSelects aggregationSelectSelects = new ConceptSelects(
				cteContext.getPrimaryColumn(),
				aggregatedValidityDate,
				aggregationFilterSelects
		);

		return QueryStep.builder()
						// pid normally
						// first value for all existing selects
						// date aggregation for date range
						// new select for all aggregation selects and filter
						.selects(aggregationSelectSelects)
						.isGroupBy(true);
	}

	@Override
	public CteStep cteStep() {
		return CteStep.AGGREGATION_SELECT;
	}

}
