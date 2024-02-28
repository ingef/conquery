package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDateUnionSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptConversionTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;

@Value
public class EventDateUnionSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	public EventDateUnionSqlAggregator(
			String alias,
			ColumnDateRange validityDate,
			ConceptConversionTables tables,
			SqlFunctionProvider functionProvider
	) {
		ColumnDateRange qualified = validityDate.qualify(tables.getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS));
		FieldWrapper<String> stringAggregation = new FieldWrapper<>(functionProvider.daterangeStringAggregation(qualified).as(alias));

		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(tables.getLastPredecessor());

		this.sqlSelects = SqlSelects.builder()
									.intervalPackingSelect(stringAggregation)
									.finalSelect(finalSelect)
									.build();
		this.whereClauses = WhereClauses.builder().build();
	}

	public static EventDateUnionSqlAggregator create(EventDateUnionSelect select, SelectContext selectContext) {
		return new EventDateUnionSqlAggregator(
				selectContext.getNameGenerator().selectName(select),
				selectContext.getValidityDate().orElseThrow(() -> new IllegalStateException("Can't convert a EventDateUnion select without a validity date")),
				selectContext.getTables(),
				selectContext.getConversionContext().getSqlDialect().getFunctionProvider()
		);
	}

}
