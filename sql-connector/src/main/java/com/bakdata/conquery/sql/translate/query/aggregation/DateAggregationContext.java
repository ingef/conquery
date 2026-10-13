package com.bakdata.conquery.sql.translate.query.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.Qualifiable;
import com.bakdata.conquery.sql.translate.ir.QualifyingUtil;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
class DateAggregationContext implements Qualifiable<DateAggregationContext> {

	SqlIdColumns ids;
	List<SqlSelect> carryThroughSelects;
	SqlTables dateAggregationTables;
	DateAggregationDates dateAggregationDates;
	@Builder.Default
	Map<DateAggregationCteStep, List<QueryStep>> intervalMergeSteps = new HashMap<>();
	SqlAggregationAction sqlAggregationAction;
	TranslatorDialect translatorDialect;

	public DateAggregationContext withStep(DateAggregationCteStep dateAggregationCteStep, QueryStep queryStep) {
		this.intervalMergeSteps.computeIfAbsent(dateAggregationCteStep, k -> new ArrayList<>())
							   .add(queryStep);
		return this;
	}

	public QueryStep getStep(DateAggregationCteStep dateAggregationCteStep) {
		List<QueryStep> querySteps = intervalMergeSteps.get(dateAggregationCteStep);
		if (querySteps != null && !querySteps.isEmpty()) {
			return querySteps.getFirst();
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
