package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Value;

@Value
public class FilterContext<V> implements Context {
	/**
	 * Filter Value
	 */
	V value;
	ConversionContext parentContext;
	SqlTables<ConceptCteStep> conceptTables;
}
