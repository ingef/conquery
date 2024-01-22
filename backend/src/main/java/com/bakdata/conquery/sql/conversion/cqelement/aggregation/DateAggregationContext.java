package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Value;
import org.jooq.Field;

@Value
@Builder(toBuilder = true)
class DateAggregationContext implements Context {

	Field<Object> primaryColumn;
	List<SqlSelect> carryThroughSelects;
	DateAggregationTables<?> dateAggregationTables;
	DateAggregationDates dateAggregationDates;
	@Builder.Default
	Map<DateAggregationCteStep, List<QueryStep>> intervalMergeSteps = new HashMap<>();
	SqlAggregationAction sqlAggregationAction;
	SqlFunctionProvider functionProvider;
	IntervalPacker intervalPacker;
	NameGenerator nameGenerator;

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
		if (dateAggregationCteStep != MergeCteStep.NODE_NO_OVERLAP) {
			throw new UnsupportedOperationException(
					"Only MergeCteStep.NODE_NO_OVERLAP has multiple steps. Use getStep() for all other DateAggregationSteps."
			);
		}
		return this.intervalMergeSteps.get(dateAggregationCteStep);
	}

	public DateAggregationContext qualify(String qualifier) {
		return this.toBuilder()
				   .primaryColumn(QualifyingUtil.qualify(this.primaryColumn, qualifier))
				   .carryThroughSelects(QualifyingUtil.qualify(this.carryThroughSelects, qualifier))
				   .dateAggregationDates(this.dateAggregationDates.qualify(qualifier))
				   .build();
	}

}
