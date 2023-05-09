package com.bakdata.conquery.sql.conversion;

import java.util.Optional;

import com.bakdata.conquery.models.query.Visitable;
import lombok.Getter;

/**
 * Determines responsibility of a {@link NodeConverter} class for a given {@link Visitable}.
 *
 * @param <V> Node of the query tree
 */
public class NodeSelector<V extends Visitable> {

	@Getter
	private final Class<V> nodeClass;

	public NodeSelector(Class<V> nodeClass) {
		this.nodeClass = nodeClass;
	}

	/**
	 * Returns the cast node if it matches the selector's class and empty otherwise.
	 */
	public Optional<V> select(Visitable visitable) {
		return nodeClass.isInstance(visitable)
			   ? Optional.of(nodeClass.cast(visitable))
			   : Optional.empty();
	}
}
