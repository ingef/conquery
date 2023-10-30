package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import lombok.Value;

@Value
public class FilterContext<V> implements Context {

	/**
	 * Filter Value
	 */
	V value;
	ConversionContext parentContext;
	ConceptTables conceptTables;

	@Override
	public NameGenerator getNameGenerator() {
		return this.parentContext.getNameGenerator();
	}
}
