package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
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
	Map<DateAggregationCteStep, QueryStep> dateAggregationSteps = new HashMap<>();
	ConversionContext conversionContext;

	public DateAggregationContext withStep(DateAggregationCteStep dateAggregationCteStep, QueryStep queryStep) {
		this.dateAggregationSteps.put(dateAggregationCteStep, queryStep);
		return this;
	}

	public QueryStep getStep(DateAggregationCteStep dateAggregationCteStep) {
		return this.dateAggregationSteps.get(dateAggregationCteStep);
	}

	@Override
	public DateAggregationContext qualify(String qualifier) {
		return this.toBuilder()
				   .ids(this.ids.qualify(qualifier))
				   .carryThroughSelects(QualifyingUtil.qualify(this.carryThroughSelects, qualifier))
				   .dateAggregationDates(this.dateAggregationDates.qualify(qualifier))
				   .build();
	}

	@Override
	public DialectBundle getDialectBundle() {
		return getConversionContext().getDialectBundle();
	}

}
