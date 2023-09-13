package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;

import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.SumSqlSelect;
import com.bakdata.conquery.sql.conversion.filter.NumberMapUtil;
import org.jooq.Field;

public class SumSelectConverter implements SelectConverter<com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect> {

	@Override
	public SqlSelects convert(com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect sumSelect, SelectContext context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumSelect.getColumn().getType());

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				sumSelect.getColumn().getName(),
				numberClass
		);

		Field<? extends Number> qualifiedRootSelect = context.getConceptTables()
															 .qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.aliased());
		SumSqlSelect sumGroupBy = new SumSqlSelect(qualifiedRootSelect, sumSelect.getName());

		ExtractingSqlSelect<? extends Number> finalSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.FINAL),
				sumGroupBy.aliased().getName(),
				numberClass
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(rootSelect))
						 .forAggregationSelectStep(Collections.singletonList(sumGroupBy))
						 .forFinalStep(Collections.singletonList(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect> getConversionClass() {
		return com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect.class;
	}

}
