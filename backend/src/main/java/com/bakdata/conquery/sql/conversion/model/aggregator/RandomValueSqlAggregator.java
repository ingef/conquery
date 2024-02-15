package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;
import org.jooq.Field;

@Value
public class RandomValueSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private RandomValueSqlAggregator(
			Column column,
			String alias,
			SqlTables<ConnectorCteStep> connectorTables,
			SqlFunctionProvider functionProvider
	) {
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(connectorTables.getRootTable(), column.getName(), Object.class);

		Field<?> qualifiedRootSelect = rootSelect.createAliasReference(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<?> randomGroupBy = new FieldWrapper<>(functionProvider.random(qualifiedRootSelect).as(alias), column.getName());

		ExtractingSqlSelect<?> finalSelect = randomGroupBy.createAliasReference(connectorTables.getPredecessor(ConnectorCteStep.FINAL));

		this.sqlSelects = SqlSelects.builder()
									.preprocessingSelect(rootSelect)
									.aggregationSelect(randomGroupBy)
									.finalSelect(finalSelect)
									.build();

		this.whereClauses = WhereClauses.empty();
	}

	public static RandomValueSqlAggregator create(RandomValueSelect randomValueSelect, SelectContext selectContext) {
		return new RandomValueSqlAggregator(
				randomValueSelect.getColumn(),
				selectContext.getNameGenerator().selectName(randomValueSelect),
				selectContext.getConnectorTables(),
				selectContext.getParentContext().getSqlDialect().getFunctionProvider()
		);
	}

}
