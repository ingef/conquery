package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;

public class FilterValueConversions {
	private final FilterConversions filterConversions;

	public FilterValueConversions(FilterConversions filterConversions) {
		this.filterConversions = filterConversions;
	}

	public ConceptFilter convert(FilterValue<?> filterValue, ConversionContext context, SqlTables<ConceptCteStep> conceptTables) {
		ConceptFilter
				convert =
				this.filterConversions.convert(filterValue.getFilter(), new FilterContext<>(filterValue.getValue(), context, conceptTables));
		if (context.isNegation()) {
			return new ConceptFilter(convert.getSelects(), convert.getFilters().negated());
		}
		return convert;
	}

	public Set<ConceptCteStep> requiredSteps(FilterValue<?> filterValue) {
		return this.filterConversions.getConverters().stream()
									 .filter(converter -> converter.getConversionClass().isInstance(filterValue.getFilter()))
									 .findFirst()
									 .orElseThrow(() -> new RuntimeException(
											 "Could not find a matching converter for filter %s. Converters: %s".formatted(filterValue.getFilter(), this.filterConversions.getConverters()))
									 )
									 .requiredSteps();
	}

}
