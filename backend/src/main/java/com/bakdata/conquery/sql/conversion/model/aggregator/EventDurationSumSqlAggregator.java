package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.math.BigDecimal;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
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
public class EventDurationSumSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private EventDurationSumSqlAggregator(
			String alias,
			ColumnDateRange validityDate,
			ConceptConversionTables tables,
			SqlFunctionProvider functionProvider
	) {
		FieldWrapper<BigDecimal> durationSumWrapper = DaterangeSelectUtil.createDurationSumSqlSelect(alias, validityDate, functionProvider);
		ExtractingSqlSelect<?> finalSelect = durationSumWrapper.qualify(tables.getLastPredecessor());

		this.sqlSelects = SqlSelects.builder()
									.eventDateSelect(durationSumWrapper)
									.finalSelect(finalSelect)
									.build();
		this.whereClauses = WhereClauses.builder().build();
	}

	public static EventDurationSumSqlAggregator create(EventDurationSumSelect eventDurationSumSelect, SelectContext selectContext) {

		ColumnDateRange validityDate = selectContext.getValidityDate().orElseThrow(
				() -> new IllegalStateException("Can't convert a EventDurationSum select without a validity date")
		);

		return new EventDurationSumSqlAggregator(
				selectContext.getNameGenerator().selectName(eventDurationSumSelect),
				prepareValidityDate(validityDate, selectContext),
				selectContext.getTables(),
				selectContext.getConversionContext().getSqlDialect().getFunctionProvider()
		);
	}

	private static ColumnDateRange prepareValidityDate(ColumnDateRange validityDate, SelectContext selectContext) {
		ColumnDateRange qualified = validityDate.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS));
		return selectContext.getSqlDialect().getFunctionProvider().toDualColumn(qualified);
	}

}
