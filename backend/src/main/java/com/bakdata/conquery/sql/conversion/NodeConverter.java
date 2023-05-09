package com.bakdata.conquery.sql.conversion;

import java.util.Optional;

import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

/**
 * Base class for converters that implement the translation of a ConQuery query to an SQL query.
 *
 * <p>
 * A ConQuery is a graph that has a {@link com.bakdata.conquery.apiv1.query.QueryDescription} as its root.
 * The children of the root are of type {@link com.bakdata.conquery.apiv1.query.CQElement}.
 *
 * @param <V> type of the node to convert
 */
public abstract class NodeConverter<V extends Visitable> {

	private final NodeSelector<V> selector;

	protected NodeConverter(Class<V> nodeClass) {
		this.selector = new NodeSelector<>(nodeClass);
	}

	public Optional<ConversionContext> convert(final Visitable queryDescription, final ConversionContext context) {
		return this.selector.select(queryDescription).map(node -> this.convertNode(node, context));
	}

	protected abstract ConversionContext convertNode(final V node, final ConversionContext context);

}
