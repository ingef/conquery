package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.Filters;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.SumGroupBy;
import org.jooq.impl.DSL;

import java.util.Collections;

public class SumConverter implements FilterConverter<IRange<? extends Number, ?>, SumFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends SumFilter> CLASS = SumFilter.class;

	@Override
	public ConceptFilter convert(SumFilter<IRange<? extends Number, ?>> sumFilter, FilterContext<IRange<? extends Number, ?>> context) {

		// TODO(tm): convert getSubtractColumn and getDistinctByColumn
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumFilter.getColumn().getType());

		ExtractingSelect<? extends Number> rootSelect = new ExtractingSelect<>(
				context.getConceptTableNames().rootTable(),
				sumFilter.getColumn().getName(),
				numberClass
		);
		SumGroupBy sumGroupBy = new SumGroupBy(rootSelect.alias());
		SumCondition sumFilterCondition = new SumCondition(DSL.field(sumGroupBy.alias()), context.getValue());

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(rootSelect))
						  .forGroupByStep(Collections.singletonList(sumGroupBy))
						  .build(),
				Filters.builder()
					   .group(Collections.singletonList(sumFilterCondition))
					   .build()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends SumFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends SumFilter<IRange<? extends Number, ?>>>) CLASS;
	}

}
