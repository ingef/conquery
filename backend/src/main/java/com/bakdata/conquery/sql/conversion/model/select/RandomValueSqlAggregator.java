package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import lombok.Value;
import org.jooq.Field;

@Value
public class RandomValueSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private RandomValueSqlAggregator(
			Column column,
			String alias,
			SqlTables<ConnectorCteStep> conceptTables,
			SqlFunctionProvider functionProvider
	) {
		String rootTableName = conceptTables.getPredecessor(ConnectorCteStep.PREPROCESSING);
		String columnName = column.getName();
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		Field<?> qualifiedRootSelect = rootSelect.createAliasedReference(conceptTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<?> randomGroupBy = new FieldWrapper<>(functionProvider.random(qualifiedRootSelect).as(alias), columnName);

		ExtractingSqlSelect<?> finalSelect = randomGroupBy.createAliasedReference(conceptTables.getPredecessor(ConnectorCteStep.FINAL));

		this.sqlSelects = SqlSelects.builder()
									.preprocessingSelect(rootSelect)
									.aggregationSelect(randomGroupBy)
									.finalSelect(finalSelect)
									.build();

		this.whereClauses = WhereClauses.builder().build();
	}

	public static RandomValueSqlAggregator create(RandomValueSelect randomValueSelect, SelectContext selectContext) {
		return new RandomValueSqlAggregator(
				randomValueSelect.getColumn(),
				selectContext.getNameGenerator().selectName(randomValueSelect),
				selectContext.getConceptTables(),
				selectContext.getParentContext().getSqlDialect().getFunctionProvider()
		);
	}

}
