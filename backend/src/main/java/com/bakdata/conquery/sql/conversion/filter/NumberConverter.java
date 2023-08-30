package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.Filters;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.NumberCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import org.jooq.impl.DSL;

import java.util.Collections;

public class NumberConverter implements FilterConverter<IRange<? extends Number, ?>, NumberFilter<IRange<? extends Number, ?>>> {
	private static final Class<? extends NumberFilter> CLASS = NumberFilter.class;

	@Override
	public ConceptFilter convert(NumberFilter<IRange<? extends Number, ?>> numberFilter, FilterContext<IRange<? extends Number, ?>> context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(numberFilter.getColumn().getType());

		ExtractingSelect<? extends Number> rootSelect = new ExtractingSelect<>(
				context.getConceptTableNames().rootTable(),
				numberFilter.getColumn().getName(),
				numberClass
		);

		String preprocessingCteName = context.getConceptTableNames().tableNameFor(CteStep.PREPROCESSING);
		NumberCondition condition = new NumberCondition(
				DSL.field(DSL.name(preprocessingCteName, rootSelect.alias().getName()), numberClass),
				context.getValue()
		);

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(rootSelect))
						  .build(),
				Filters.builder()
					   .event(Collections.singletonList(condition))
					   .build()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends NumberFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends NumberFilter<IRange<? extends Number, ?>>>) CLASS;
	}
}
