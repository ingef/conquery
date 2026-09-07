package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;

/**
 * Marker for a conversion context.
 */
public interface Context {

	ConversionContext getConversionContext();

	LegacyCompilerDialect getCompilerDialect();

	default SqlNameGenerator getNameGenerator() {
		return getConversionContext().getNameGenerator();
	}
	
	default SqlFunctionProvider getFunctionProvider(){
		return getConversionContext().getCompilerDialect().getFunctionProvider();
	}

}
