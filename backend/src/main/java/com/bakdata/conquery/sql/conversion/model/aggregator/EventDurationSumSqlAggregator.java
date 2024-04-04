package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

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
import org.jooq.impl.DSL;

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
		ColumnDateRange qualified = validityDate.qualify(tables.getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS));
		ColumnDateRange asDualColumn = functionProvider.toDualColumn(qualified);
		FieldWrapper<BigDecimal> durationSum = new FieldWrapper<>(
				DSL.sum(functionProvider.dateDistance(ChronoUnit.DAYS, asDualColumn.getStart(), asDualColumn.getEnd()))
				   .as(alias)
		);

		ExtractingSqlSelect<?> finalSelect = durationSum.qualify(tables.getLastPredecessor());

		this.sqlSelects = SqlSelects.builder()
									.intervalPackingSelect(durationSum)
									.finalSelect(finalSelect)
									.build();
		this.whereClauses = WhereClauses.builder().build();
	}

	public static EventDurationSumSqlAggregator create(EventDurationSumSelect eventDurationSumSelect, SelectContext selectContext) {
		return new EventDurationSumSqlAggregator(
				selectContext.getNameGenerator().selectName(eventDurationSumSelect),
				selectContext.getValidityDate().orElseThrow(() -> new IllegalStateException("Can't convert a EventDurationSum select without a validity date")),
				selectContext.getTables(),
				selectContext.getConversionContext().getSqlDialect().getFunctionProvider()
		);
	}

}
