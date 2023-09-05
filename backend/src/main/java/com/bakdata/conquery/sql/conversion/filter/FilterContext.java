package com.bakdata.conquery.sql.conversion.filter;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import lombok.Value;

@Value
public class FilterContext<V> implements Context {
	/**
	 * Filter Value
	 */
	V value;
	ConversionContext parentContext;
	ConceptTables conceptTables;
}
