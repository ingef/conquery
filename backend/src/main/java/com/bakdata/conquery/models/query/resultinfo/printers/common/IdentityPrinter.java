package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record IdentityPrinter() implements Printer {

	@Override
	public Object apply(Object value) {
		return value;
	}
}
