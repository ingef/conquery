package com.bakdata.conquery.models.query;

import java.util.function.Consumer;
import java.util.stream.Stream;

public interface Visitable {

	void visit(Consumer<Visitable> visitor);

	/**
	 * Creates a stream of all elements in the Visitable.
	 */
	public static Stream<Visitable> stream(Visitable visitable) {
		final Stream.Builder<Visitable> builder = Stream.builder();

		visitable.visit(builder);

		return builder.build();
	}
	
}
