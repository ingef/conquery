package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTableNames;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;

public class FilterValueConversions {
	private final FilterConversions filterConversions;

	public FilterValueConversions(FilterConversions filterConversions) {
		this.filterConversions = filterConversions;
	}

	public ConceptFilter convert(FilterValue<?> filterValue, ConversionContext context, ConceptTableNames nameGenerator) {
		ConceptFilter convert = this.filterConversions.convert(filterValue.getFilter(), new FilterContext<>(filterValue.getValue(), context, nameGenerator));
		if (context.isNegation()) {
			return new ConceptFilter(convert.getSelects(), convert.getFilters().negated());
		}
		return convert;
	}

}
