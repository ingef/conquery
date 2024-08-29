package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.function.Function;

@FunctionalInterface
public interface Printer extends Function<Object, Object> {
	Object apply(Object value);
}
