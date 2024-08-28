package com.bakdata.conquery.models.query.resultinfo.printers;

record IdentityPrinter() implements Printer {

	@Override
	public Object print(Object value) {
		return value;
	}
}
