package com.bakdata.conquery.models.query.resultinfo.printers;

@FunctionalInterface
public interface Printer {
	Object print(Object value);
}
