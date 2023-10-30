package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitExtractingSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.RandomValueSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class RandomValueSelectConverter implements SelectConverter<RandomValueSelect> {

	@Override
	public SqlSelects convert(RandomValueSelect randomSelect, SelectContext context) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				randomSelect.getColumn().getName(),
				Object.class
		);

		SqlSelect randomValueSqlSelect =
				RandomValueSqlSelect.builder()
									.randomColumn(context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased()))
									.alias(context.getNameGenerator().selectName(randomSelect))
									.functionProvider(context.getParentContext().getSqlDialect().getFunctionProvider())
									.build();

		ExplicitSelect finalSelect = ExplicitExtractingSelect.fromSelect(
				randomSelect,
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
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
