package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;

/**
 * Marker for a conversion context.
 */
public interface Context {

	ConversionContext getConversionContext();

	default DialectBundle getSqlDialect() {
		return getConversionContext().getSqlDialect();
	}

	default NameGenerator getNameGenerator() {
		return getConversionContext().getNameGenerator();
	}

}
