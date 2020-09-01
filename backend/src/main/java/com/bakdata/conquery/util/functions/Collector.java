package com.bakdata.conquery.util.functions;

@FunctionalInterface
public interface Collector<KEY, VALUE> {
	void accept(KEY key, VALUE value);
	
	default Collector<KEY, VALUE> collect(KEY key,VALUE value) {
		accept(key, value);
		return this;
	}
}
