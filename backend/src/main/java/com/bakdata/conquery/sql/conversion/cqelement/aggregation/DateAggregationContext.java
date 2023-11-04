package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import lombok.Builder;
import lombok.Value;
import org.jooq.Field;

@Value
@Builder(toBuilder = true)
class DateAggregationContext implements Context {

	Field<Object> primaryColumn;
	List<ExplicitSelect> carryThroughSelects;
	DateAggregationTables dateAggregationTables;
	DateAggregationDates dateAggregationDates;
	@Builder.Default
	Map<DateAggregationStep, List<QueryStep>> intervalMergeSteps = new HashMap<>();
	SqlAggregationAction sqlAggregationAction;
	SqlFunctionProvider functionProvider;
	IntervalPacker intervalPacker;

	public DateAggregationContext withStep(DateAggregationStep dateAggregationStep, QueryStep queryStep) {
		this.intervalMergeSteps.computeIfAbsent(dateAggregationStep, k -> new ArrayList<>())
							   .add(queryStep);
		return this;
	}

	public QueryStep getStep(DateAggregationStep dateAggregationStep) {
		List<QueryStep> querySteps = intervalMergeSteps.get(dateAggregationStep);
		if (querySteps != null && !querySteps.isEmpty()) {
			return querySteps.get(0);
		}
		return null;
	}

	public List<QueryStep> getSteps(DateAggregationStep dateAggregationStep) {
		if (dateAggregationStep != MergeStep.NODE_NO_OVERLAP) {
			throw new UnsupportedOperationException(
					"Only MergeStep.NODE_NO_OVERLAP has multiple steps. Use getStep() for all other DateAggregationSteps."
			);
		}
		return this.intervalMergeSteps.get(dateAggregationStep);
	}

	public DateAggregationContext qualify(String qualifier) {
		return this.toBuilder()
				   .primaryColumn(QualifyingUtil.qualify(this.primaryColumn, qualifier))
				   .carryThroughSelects(QualifyingUtil.qualify(this.carryThroughSelects, qualifier, ExplicitSelect.class))
				   .dateAggregationDates(this.dateAggregationDates.qualify(qualifier))
				   .build();
	}

}
