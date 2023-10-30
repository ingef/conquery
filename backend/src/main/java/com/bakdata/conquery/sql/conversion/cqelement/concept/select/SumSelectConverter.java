package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitExtractingSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.SumSqlSelect;

public class SumSelectConverter implements SelectConverter<SumSelect> {

	@Override
	public SqlSelects convert(SumSelect sumSelect, SelectContext context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumSelect.getColumn().getType());

		SumSqlSelect sumGroupBy = SumSqlSelect.create(
				sumSelect,
				context.getNameGenerator().selectName(sumSelect),
				context.getConceptTables()
		);

		ExplicitSelect finalSelect = ExplicitExtractingSelect.fromSelect(
				sumSelect,
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
				sumGroupBy.aliased().getName(),
				numberClass
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(sumGroupBy.getPreprocessingSelects())
						 .forAggregationSelectStep(List.of(sumGroupBy))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends SumSelect> getConversionClass() {
		return SumSelect.class;
	}

}
