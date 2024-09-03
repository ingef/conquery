package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.function.Function;

@FunctionalInterface
public interface Printer<T> extends Function<T, Object> {
	Object apply(T value);
}
