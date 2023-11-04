package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.PrefixTextFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.PrefixTextCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class PrefixTextFilterConverter implements FilterConverter<String, PrefixTextFilter> {

	@Override
	public ConceptFilter convert(PrefixTextFilter prefixTextFilter, FilterContext<String> context) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				prefixTextFilter.getColumn().getName(),
				String.class
		);

		Field<String> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessorTableName(ConceptCteStep.EVENT_FILTER, rootSelect.aliased());
		PrefixTextCondition prefixTextCondition = new PrefixTextCondition(qualifiedRootSelect, context.getValue());

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(rootSelect))
						  .build(),
				Filters.builder()
					   .event(Collections.singletonList(prefixTextCondition))
					   .build()
		);
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.EVENT_FILTER);
	}

	@Override
	public Class<PrefixTextFilter> getConversionClass() {
		return PrefixTextFilter.class;
	}

}
