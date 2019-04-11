package com.bakdata.conquery.util.functions;

@FunctionalInterface
public interface Collector<VALUE> {
	void accept(VALUE value);
	
	default Collector<VALUE> collect(VALUE value) {
		accept(value);
		return this;
	}
}
