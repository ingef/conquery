package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Value;

@Value
public class FilterContext<V> implements Context {

	/**
	 * A filter value ({@link FilterValue#getValue()})
	 */
	V value;
	ConversionContext parentContext;
	SqlTables<ConceptCteStep> conceptTables;

	@Override
	public NameGenerator getNameGenerator() {
		return this.parentContext.getNameGenerator();
	}

}
