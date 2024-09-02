package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Objects;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record ToStringPrinter(Printer delegate) implements Printer {

	@Override
	public Object apply(Object value) {
		return Objects.toString(delegate.apply(value));
	}
}
