package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.LastValueSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class LastValueSelectConverter implements SelectConverter<LastValueSelect> {

	@Override
	public SqlSelects convert(LastValueSelect lastSelect, SelectContext context) {

		String rootTableName = context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING);
		String columName = lastSelect.getColumn().getName();
		SqlSelect rootSelect = new ExtractingSqlSelect<>(rootTableName, columName, Object.class);

		List<Field<?>> validityDateFields = context.getValidityDate()
												   .map(validityDate -> validityDate.qualify(context.getConceptTables()
																									.getPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT)))
												   .map(ColumnDateRange::toFields)
												   .orElse(Collections.emptyList());

		Field<Object> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		String alias = lastSelect.getName();
		SqlSelect lastValueSqlSelect = LastValueSqlSelect.builder()
														 .lastColumn(qualifiedRootSelect)
														 .alias(alias)
														 .orderByColumns(validityDateFields)
														 .functionProvider(context.getParentContext().getSqlDialect().getFunctionProvider())
														 .build();

		ExtractingSqlSelect<Object> finalSelect = new ExtractingSqlSelect<>(
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
