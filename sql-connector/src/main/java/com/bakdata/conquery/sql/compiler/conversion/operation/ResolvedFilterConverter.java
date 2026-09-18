package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.compiler.conversion.ConversionDispatcher;
import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;

/** Converts resolved filter-model values into connector filter IR. */
public final class ResolvedFilterConverter {

	private final ConversionDispatcher<ResolvedFilter, SqlFilters, FilterConversionContext> dispatcher;

	public ResolvedFilterConverter() {
		this(List.of());
	}

	/**
	 * Creates a converter with the built-in column-filter implementations and additional extension converters.
	 *
	 * <p>An extension must support a distinct runtime type; ambiguous converters are rejected by the dispatcher.</p>
	 */
	public ResolvedFilterConverter(
			List<? extends Converter<? extends ResolvedFilter, SqlFilters, FilterConversionContext>> extensions
	) {
		List<Converter<? extends ResolvedFilter, SqlFilters, FilterConversionContext>> converters = new ArrayList<>(
				BuiltInColumnFilterConverters.create()
		);
		converters.addAll(extensions);
		this.dispatcher = new ConversionDispatcher<>(converters);
	}

	public SqlFilters convert(ResolvedFilter filter, FilterConversionContext context) {
		return dispatcher.convert(filter, context);
	}
}
