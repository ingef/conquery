package com.bakdata.conquery.models.query.resultinfo.printers;

public record ChainingPrinter(Printer incoming, Printer outgoing) implements Printer {

	@Override
	public Object print(Object value) {
		return outgoing.print(incoming.print(value));
	}
}
