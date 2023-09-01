package com.bakdata.conquery.sql.conversion.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.SumGroupBy;
import com.bakdata.conquery.sql.conversion.filter.NumberMapUtil;

public class SumConverter implements SelectConverter<SumSelect> {

	@Override
	public SqlSelects convert(SumSelect sumSelect, SelectContext context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumSelect.getColumn().getType());

		ExtractingSelect<? extends Number> rootSelect = new ExtractingSelect<>(
				context.getTables().rootTable(),
				sumSelect.getColumn().getName(),
				numberClass
		);

		SumGroupBy sumGroupBy = new SumGroupBy(
				rootSelect.alias()
		);

		ExtractingSelect<? extends Number> finalSelect = new ExtractingSelect<>(
				context.getTables().tableNameFor(CteStep.AGGREGATION_SELECT),
				sumGroupBy.alias().getName(),
				numberClass
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(sumGroupBy))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends SumSelect> getConversionClass() {
		return SumSelect.class;
	}

}
