package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import org.jooq.Field;

public class RandomValueSelectConverter implements SelectConverter<RandomValueSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(RandomValueSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {

		ConnectorSqlTables tables = selectContext.getTables();

		String rootTableName = tables.getRootTable();
		String columnName = select.getColumn().getName();
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		String alias = selectContext.getNameGenerator().selectName(select);
		Field<?> qualifiedRootSelect = rootSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		Field<?> firstAggregation = selectContext.getFunctionProvider().random(qualifiedRootSelect).as(alias);
		FieldWrapper<?> firstAggregationSqlSelect = new FieldWrapper<>(firstAggregation, columnName);

		ExtractingSqlSelect<?> finalSelect = firstAggregationSqlSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(rootSelect)
								  .aggregationSelect(firstAggregationSqlSelect)
								  .finalSelect(finalSelect)
								  .build();
	}
}
