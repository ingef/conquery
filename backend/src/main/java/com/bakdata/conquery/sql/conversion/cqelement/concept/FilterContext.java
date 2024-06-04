package com.bakdata.conquery.sql.conversion.cqelement.concept;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterContext<V> implements Context {

	SqlIdColumns ids;

	/**
	 * A filter value ({@link FilterValue#getValue()})
	 */
	V value;

	ConversionContext conversionContext;

	/**
	 * Not present if this context is for table export.
	 */
	@Nullable
	ConceptConversionTables tables;

	public static <V> FilterContext<V> forConceptConversion(SqlIdColumns ids, V value, ConversionContext conversionContext, ConceptConversionTables tables) {
		return new FilterContext<>(ids, value, conversionContext, tables);
	}

	public static <V> FilterContext<V> forTableExport(SqlIdColumns ids, V value, ConversionContext conversionContext) {
		return new FilterContext<>(ids, value, conversionContext, null);
	}

}
