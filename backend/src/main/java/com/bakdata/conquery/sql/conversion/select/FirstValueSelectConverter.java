package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.FirstValueSqlSelect;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class FirstValueSelectConverter implements SelectConverter<com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect> {

	@Override
	public SqlSelects convert(com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect firstSelect, SelectContext context) {

		ExtractingSqlSelect<Object> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				firstSelect.getColumn().getName(),
				Object.class
		);


		List<Field<?>> validityDateFields = context.getValidityDate()
												   .map(validityDate -> validityDate.qualify(context.getConceptTables()
																									.getPredecessorTableName(CteStep.AGGREGATION_SELECT)))
												   .map(ColumnDateRange::toFields)
												   .orElse(List.of(DSL.field(DSL.val(1))));

		FirstValueSqlSelect firstValueSqlSelect =
				FirstValueSqlSelect.builder()
								   .firstColumn(context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.aliased()))
								   .alias(firstSelect.getName())
								   .orderByColumns(validityDateFields)
								   .functionProvider(context.getParentContext().getSqlDialect().getFunction())
								   .build();


		ExtractingSqlSelect<Object> finalSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.FINAL),
				firstValueSqlSelect.aliased().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(rootSelect))
						 .forAggregationSelectStep(Collections.singletonList(firstValueSqlSelect))
						 .forFinalStep(Collections.singletonList(finalSelect))
						 .build();
	}

	@Override
	public Class<com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect> getConversionClass() {
		return com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect.class;
	}

}
