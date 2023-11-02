package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FirstValueSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class FirstValueSelectConverter implements SelectConverter<FirstValueSelect> {

	@Override
	public SqlSelects convert(FirstValueSelect firstSelect, SelectContext context) {

		ExtractingSqlSelect<Object> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				firstSelect.getColumn().getName(),
				Object.class
		);


		List<Field<?>> validityDateFields = context.getValidityDate()
												   .map(validityDate -> validityDate.qualify(context.getConceptTables()
																									.getPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT)))
												   .map(ColumnDateRange::toFields)
												   .orElse(List.of(DSL.field(DSL.val(1))));

		FirstValueSqlSelect firstValueSqlSelect =
				FirstValueSqlSelect.builder()
								   .firstColumn(context.getConceptTables()
													   .qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased()))
								   .alias(firstSelect.getName())
								   .orderByColumns(validityDateFields)
								   .functionProvider(context.getParentContext().getSqlDialect().getFunctionProvider())
								   .build();


		ExtractingSqlSelect<Object> finalSelect = new ExtractingSqlSelect<>(
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
