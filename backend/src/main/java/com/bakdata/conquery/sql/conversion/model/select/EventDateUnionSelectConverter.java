package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDateUnionSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.google.common.base.Preconditions;

public class EventDateUnionSelectConverter implements SelectConverter<EventDateUnionSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(EventDateUnionSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {

		FieldWrapper<String> stringAggregation = createEventDateUnionAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .eventDateSelect(stringAggregation)
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public ConceptSqlSelects conceptSelect(EventDateUnionSelect select, SelectContext<TreeConcept, ConceptSqlTables> selectContext) {

		FieldWrapper<String> stringAggregation = createEventDateUnionAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.UNIVERSAL_SELECTS));

		return ConceptSqlSelects.builder()
								.eventDateSelect(stringAggregation)
								.finalSelect(finalSelect)
								.build();
	}

	private static FieldWrapper<String> createEventDateUnionAggregation(EventDateUnionSelect select, SelectContext<?, ?> selectContext) {

		Preconditions.checkArgument(selectContext.getValidityDate().isPresent(), "Can't convert an EventDateUnionSelect without a validity date being present");
		ColumnDateRange validityDate = selectContext.getValidityDate().get();

		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		String alias = selectContext.getNameGenerator().selectName(select);

		ColumnDateRange qualified = validityDate.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS));
		return new FieldWrapper<>(functionProvider.daterangeStringAggregation(qualified).as(alias));
	}

}
