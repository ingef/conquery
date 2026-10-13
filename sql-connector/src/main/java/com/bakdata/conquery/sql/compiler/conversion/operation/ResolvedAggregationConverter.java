package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.compiler.conversion.ConversionDispatcher;
import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;

/** Converts resolved aggregation-model values into connector aggregation IR. */
public final class ResolvedAggregationConverter {

	private final ConversionDispatcher<ResolvedAggregation, CommonAggregationSelect<?>, AggregationConversionContext> dispatcher;

	public ResolvedAggregationConverter() {
		this(List.of());
	}

	/**
	 * Creates a converter with the built-in aggregation implementations and additional extension converters.
	 *
	 * <p>An extension must support a distinct runtime type; ambiguous converters are rejected by the dispatcher.</p>
	 */
	public ResolvedAggregationConverter(
			List<? extends Converter<? extends ResolvedAggregation, CommonAggregationSelect<?>, AggregationConversionContext>> extensions
	) {
		List<Converter<? extends ResolvedAggregation, CommonAggregationSelect<?>, AggregationConversionContext>> converters =
				new ArrayList<>(BuiltInAggregationConverters.create());
		converters.addAll(extensions);
		this.dispatcher = new ConversionDispatcher<>(converters);
	}

	public CommonAggregationSelect<?> convert(ResolvedAggregation aggregation, AggregationConversionContext context) {
		return dispatcher.convert(aggregation, context);
	}
}
