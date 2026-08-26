package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;

/** Recursively dispatches a query node to its matching SQL converter. */
@FunctionalInterface
public interface NodeConversionDispatcher {

	ConversionContext convert(Object node, ConversionContext context);
}
