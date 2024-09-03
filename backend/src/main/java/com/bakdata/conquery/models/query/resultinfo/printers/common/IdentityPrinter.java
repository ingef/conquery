package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record IdentityPrinter<T>() implements Printer<T> {

	@Override
	public Object apply(T value) {
		return value;
	}
}
