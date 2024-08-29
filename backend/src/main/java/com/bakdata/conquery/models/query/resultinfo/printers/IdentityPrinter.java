package com.bakdata.conquery.models.query.resultinfo.printers;

record IdentityPrinter() implements Printer {

	@Override
	public Object apply(Object value) {
		return value;
	}
}
