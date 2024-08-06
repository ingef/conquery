package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import org.jooq.Field;

class ValueSelectUtil {

	public static ConnectorSqlSelects createValueSelect(
			Column column,
			String alias,
			BiFunction<Field<?>, List<Field<?>>, Field<?>> aggregationFunction,
			SelectContext<ConnectorSqlTables> selectContext
	) {
		ConnectorSqlTables tables = selectContext.getTables();

		String rootTableName = tables.getRootTable();
		String columnName = column.getName();
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		List<Field<?>> validityDateFields =
				selectContext.getValidityDate().map(dateRange -> dateRange.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)))
							 .map(ColumnDateRange::toFields)
							 .orElse(Collections.emptyList());

		Field<?> qualifiedRootSelect = rootSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		Field<?> firstAggregation = aggregationFunction.apply(qualifiedRootSelect, validityDateFields).as(alias);
		FieldWrapper<?> firstAggregationSqlSelect = new FieldWrapper<>(firstAggregation, columnName);

		ExtractingSqlSelect<?> finalSelect = firstAggregationSqlSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(rootSelect)
								  .aggregationSelect(firstAggregationSqlSelect)
								  .finalSelect(finalSelect)
								  .build();
	}

}
