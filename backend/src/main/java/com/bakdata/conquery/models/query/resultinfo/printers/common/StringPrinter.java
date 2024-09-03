package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Objects;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record StringPrinter() implements Printer<String> {
	@Override
	public String apply(String f) {
		return Objects.toString(f);
	}
}
