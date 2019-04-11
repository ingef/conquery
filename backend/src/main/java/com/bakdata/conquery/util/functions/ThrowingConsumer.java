package com.bakdata.conquery.util.functions;

@FunctionalInterface
public interface ThrowingConsumer<VALUE> {
	void accept(VALUE value) throws Exception;
}
