package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitExtractingSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FirstValueSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class FirstValueSelectConverter implements SelectConverter<FirstValueSelect> {

	@Override
	public SqlSelects convert(FirstValueSelect firstSelect, SelectContext context) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				firstSelect.getColumn().getName(),
				Object.class
		);

		List<Field<?>> validityDateFields = context.getValidityDate()
												   .map(validityDate -> validityDate.qualify(context.getConceptTables()
																									.getPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT)))
												   .map(ColumnDateRange::toFields)
												   .orElse(Collections.emptyList());

		SqlSelect firstValueSqlSelect =
				FirstValueSqlSelect.builder()
								   .firstColumn(context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased()))
								   .alias(firstSelect.getName())
								   .orderByColumns(validityDateFields)
								   .functionProvider(context.getParentContext().getSqlDialect().getFunctionProvider())
								   .build();

		ExplicitSelect finalSelect = ExplicitExtractingSelect.fromSelect(
				firstSelect,
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
				firstValueSqlSelect.aliased().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(firstValueSqlSelect))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<FirstValueSelect> getConversionClass() {
		return FirstValueSelect.class;
	}

}
