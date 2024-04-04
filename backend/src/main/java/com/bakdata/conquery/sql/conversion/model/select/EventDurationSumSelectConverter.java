package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.google.common.base.Preconditions;

public class EventDurationSumSelectConverter implements SelectConverter<EventDurationSumSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(EventDurationSumSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {

		FieldWrapper<BigDecimal> durationSum = createEventDurationSumAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = durationSum.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .eventDateSelect(durationSum)
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public ConceptSqlSelects conceptSelect(EventDurationSumSelect select, SelectContext<TreeConcept, ConceptSqlTables> selectContext) {

		FieldWrapper<BigDecimal> durationSum = createEventDurationSumAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = durationSum.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.UNIVERSAL_SELECTS));

		return ConceptSqlSelects.builder()
								.eventDateSelect(durationSum)
								.finalSelect(finalSelect)
								.build();
	}

	private static FieldWrapper<BigDecimal> createEventDurationSumAggregation(EventDurationSumSelect select, SelectContext<?, ?> selectContext) {

		String alias = selectContext.getNameGenerator().selectName(select);
		ColumnDateRange validityDate = prepareValidityDate(selectContext);
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();

		return DaterangeSelectUtil.createDurationSumSqlSelect(alias, validityDate, functionProvider);
	}

	private static ColumnDateRange prepareValidityDate(SelectContext<?, ?> selectContext) {
		Preconditions.checkArgument(selectContext.getValidityDate().isPresent(), "Can't convert an EventDurationSum without a validity date being present");
		ColumnDateRange validityDate = selectContext.getValidityDate().get();
		ColumnDateRange qualified = validityDate.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS));
		return selectContext.getSqlDialect().getFunctionProvider().toDualColumn(qualified);
	}

}
