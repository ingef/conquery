package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitExtractingSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.LastValueSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class LastValueSelectConverter implements SelectConverter<LastValueSelect> {

	@Override
	public SqlSelects convert(LastValueSelect lastSelect, SelectContext context) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				lastSelect.getColumn().getName(),
				Object.class
		);

		List<Field<?>> validityDateFields = context.getValidityDate()
												   .map(validityDate -> validityDate.qualify(context.getConceptTables()
																									.getPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT)))
												   .map(ColumnDateRange::toFields)
												   .orElse(Collections.emptyList());

		SqlSelect lastValueSqlSelect =
				LastValueSqlSelect.builder()
								  .lastColumn(context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased()))
								  .alias(context.getNameGenerator().selectName(lastSelect))
								  .orderByColumns(validityDateFields)
								  .functionProvider(context.getParentContext().getSqlDialect().getFunctionProvider())
								  .build();

		ExplicitSelect finalSelect = ExplicitExtractingSelect.fromSelect(
				lastSelect,
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
				lastValueSqlSelect.aliased().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(lastValueSqlSelect))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<LastValueSelect> getConversionClass() {
		return LastValueSelect.class;
	}

}
