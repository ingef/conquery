package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.SumGroupBy;
import com.bakdata.conquery.sql.conversion.filter.NumberMapUtil;
import org.jooq.Field;

public class SumSelectConverter implements SelectConverter<SumSelect> {

	@Override
	public SqlSelects convert(SumSelect sumSelect, SelectContext context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumSelect.getColumn().getType());

		ExtractingSelect<? extends Number> rootSelect = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				sumSelect.getColumn().getName(),
				numberClass
		);

		Field<? extends Number> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.alias());
		SumGroupBy sumGroupBy = new SumGroupBy(qualifiedRootSelect);

		ExtractingSelect<? extends Number> finalSelect = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.FINAL),
				sumGroupBy.alias().getName(),
				numberClass
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(rootSelect))
						 .forAggregationSelectStep(Collections.singletonList(sumGroupBy))
						 .forFinalStep(Collections.singletonList(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends SumSelect> getConversionClass() {
		return SumSelect.class;
	}

}
