package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.FirstValueGroupBy;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class FirstValueConverter implements SelectConverter<FirstValueSelect> {

	@Override
	public SqlSelects convert(FirstValueSelect convert, SelectContext context) {

		ExtractingSelect<Object> rootSelect = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				convert.getColumn().getName(),
				Object.class
		);

		List<Field<?>> validityDateFields = context.getValidityDate()
												   .map(validityDate -> validityDate.qualify(context.getConceptTables()
																									.getPredecessorTableName(CteStep.AGGREGATION_SELECT)))
												   .map(ColumnDateRange::toFields)
												   .orElse(List.of(DSL.field("1")));

		FirstValueGroupBy firstValueGroupBy = new FirstValueGroupBy(
				context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.alias()),
				validityDateFields,
				context.getParentContext().getSqlDialect().getFunction()
		);

		ExtractingSelect<Object> finalSelect = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.FINAL),
				firstValueGroupBy.alias().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(rootSelect))
						 .forAggregationSelectStep(Collections.singletonList(firstValueGroupBy))
						 .forFinalStep(Collections.singletonList(finalSelect))
						 .build();
	}

	@Override
	public Class<FirstValueSelect> getConversionClass() {
		return FirstValueSelect.class;
	}

}
