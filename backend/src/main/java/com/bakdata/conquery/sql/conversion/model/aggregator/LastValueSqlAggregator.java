package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;
import org.jooq.Field;

@Value
public class LastValueSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private LastValueSqlAggregator(
			Column column,
			String alias,
			Optional<ColumnDateRange> validityDate,
			SqlTables<ConnectorCteStep> connectorTables,
			SqlFunctionProvider functionProvider
	) {
		String rootTableName = connectorTables.getPredecessor(ConnectorCteStep.PREPROCESSING);
		String columnName = column.getName();
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		List<Field<?>> validityDateFields =
				validityDate.map(_validityDate -> _validityDate.qualify(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT)))
							.map(ColumnDateRange::toFields)
							.orElse(Collections.emptyList());
		Field<?> qualifiedRootSelect = rootSelect.createAliasReference(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<?> lastGroupBy = new FieldWrapper<>(functionProvider.last(qualifiedRootSelect, validityDateFields).as(alias), columnName);

		ExtractingSqlSelect<?> finalSelect = lastGroupBy.createAliasReference(connectorTables.getPredecessor(ConnectorCteStep.FINAL));

		this.sqlSelects = SqlSelects.builder()
									.preprocessingSelect(rootSelect)
									.aggregationSelect(lastGroupBy)
									.finalSelect(finalSelect)
									.build();

		this.whereClauses = WhereClauses.builder().build();
	}

	public static LastValueSqlAggregator create(LastValueSelect lastValueSelect, SelectContext selectContext) {
		return new LastValueSqlAggregator(
				lastValueSelect.getColumn(),
				selectContext.getNameGenerator().selectName(lastValueSelect),
				selectContext.getValidityDate(),
				selectContext.getConnectorTables(),
				selectContext.getParentContext().getSqlDialect().getFunctionProvider()
		);
	}

}
