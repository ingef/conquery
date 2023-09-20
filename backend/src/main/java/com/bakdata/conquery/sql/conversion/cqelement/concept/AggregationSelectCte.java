package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.ConceptSelects;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class AggregationSelectCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		// all selects that are required in the aggregation filter step
		String previousCteName = cteContext.getPrevious().getCteName();
		List<SqlSelect> aggregationFilterSelects = cteContext.allConceptSelects()
															 .flatMap(sqlSelects -> sqlSelects.getForAggregationSelectStep().stream())
															 .distinct()
															 .collect(Collectors.toList());

		SqlFunctionProvider functionProvider = cteContext.getContext().getSqlDialect().getFunction();
		Optional<ColumnDateRange> aggregatedValidityDate = cteContext.getValidityDateRange()
																	 .map(validityDate -> validityDate.qualify(previousCteName))
																	 .map(functionProvider::aggregated)
																	 .map(validityDate -> validityDate.asValidityDateRange(cteContext.getConceptLabel()));

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
