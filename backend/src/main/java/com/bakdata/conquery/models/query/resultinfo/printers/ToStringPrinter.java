package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.TextNode;

public record ToStringPrinter(Printer delegate) implements Printer {

	@Override
	public Object apply(Object value) {
		return new TextNode(Objects.toString(delegate.apply(value)));
	}
}
