package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;
import org.jooq.Field;

@Value
public class FirstValueSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private FirstValueSqlAggregator(
			Column column,
			String alias,
			Optional<ColumnDateRange> validityDate,
			SqlTables connectorTables,
			SqlFunctionProvider functionProvider
	) {
		String rootTableName = connectorTables.getRootTable();
		String columnName = column.getName();
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		List<Field<?>> validityDateFields =
				validityDate.map(_validityDate -> _validityDate.qualify(connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)))
							.map(ColumnDateRange::toFields)
							.orElse(Collections.emptyList());
		Field<?> qualifiedRootSelect = rootSelect.qualify(connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<?> firstGroupBy = new FieldWrapper<>(functionProvider.first(qualifiedRootSelect, validityDateFields).as(alias), columnName);

		ExtractingSqlSelect<?> finalSelect = firstGroupBy.qualify(connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		this.sqlSelects = SqlSelects.builder()
									.preprocessingSelect(rootSelect)
									.aggregationSelect(firstGroupBy)
									.finalSelect(finalSelect)
									.build();

		this.whereClauses = WhereClauses.empty();
	}

	public static FirstValueSqlAggregator create(FirstValueSelect firstValueSelect, SelectContext selectContext) {
		return new FirstValueSqlAggregator(
				firstValueSelect.getColumn(),
				selectContext.getNameGenerator().selectName(firstValueSelect),
				selectContext.getValidityDate(),
				selectContext.getTables(),
				selectContext.getConversionContext().getSqlDialect().getFunctionProvider()
		);
	}

}
