package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.Qualifiable;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
class DateAggregationContext implements Context, Qualifiable<DateAggregationContext> {

	SqlIdColumns ids;
	List<SqlSelect> carryThroughSelects;
	SqlTables dateAggregationTables;
	DateAggregationDates dateAggregationDates;
	@Builder.Default
	Map<DateAggregationCteStep, List<QueryStep>> intervalMergeSteps = new HashMap<>();
	SqlAggregationAction sqlAggregationAction;
	ConversionContext conversionContext;

	public DateAggregationContext withStep(DateAggregationCteStep dateAggregationCteStep, QueryStep queryStep) {
		this.intervalMergeSteps.computeIfAbsent(dateAggregationCteStep, k -> new ArrayList<>())
							   .add(queryStep);
		return this;
	}

	public QueryStep getStep(DateAggregationCteStep dateAggregationCteStep) {
		List<QueryStep> querySteps = intervalMergeSteps.get(dateAggregationCteStep);
		if (querySteps != null && !querySteps.isEmpty()) {
			return querySteps.get(0);
		}
		return null;
	}

	public List<QueryStep> getSteps(DateAggregationCteStep dateAggregationCteStep) {
		if (dateAggregationCteStep != DateAggregationCteStep.NODE_NO_OVERLAP) {
			throw new UnsupportedOperationException(
					"Only MergeCteStep.NODE_NO_OVERLAP has multiple steps. Use getStep() for all other DateAggregationSteps."
			);
		}
		return this.intervalMergeSteps.get(dateAggregationCteStep);
	}

	@Override
	public DateAggregationContext qualify(String qualifier) {
		return this.toBuilder()
				   .ids(this.ids.qualify(qualifier))
				   .carryThroughSelects(QualifyingUtil.qualify(this.carryThroughSelects, qualifier))
				   .dateAggregationDates(this.dateAggregationDates.qualify(qualifier))
				   .build();
	}

}
