package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.RandomValueSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class RandomValueSelectConverter implements SelectConverter<RandomValueSelect> {

	@Override
	public SqlSelects convert(RandomValueSelect randomSelect, SelectContext context) {

		String rootTableName = context.getConceptTables().getPredecessor(ConceptCteStep.PREPROCESSING);
		String columnName = randomSelect.getColumn().getName();
		SqlSelect rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		Field<Object> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		String alias = randomSelect.getName();
		SqlSelect randomValueSqlSelect = RandomValueSqlSelect.builder()
															 .randomColumn(qualifiedRootSelect)
															 .alias(alias)
															 .functionProvider(context.getParentContext().getSqlDialect().getFunctionProvider())
															 .build();

		ExtractingSqlSelect<Object> finalSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessor(ConceptCteStep.FINAL),
				randomValueSqlSelect.aliased().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(randomValueSqlSelect))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<RandomValueSelect> getConversionClass() {
		return RandomValueSelect.class;
	}


}
