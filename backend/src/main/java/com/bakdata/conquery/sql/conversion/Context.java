package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;

/**
 * Marker for a conversion context.
 */
public interface Context {

	ConversionContext getConversionContext();

	default SqlDialect getSqlDialect() {
		return getConversionContext().getSqlDialect();
	}

	default NameGenerator getNameGenerator() {
		return getConversionContext().getNameGenerator();
	}

}
