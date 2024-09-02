package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.List;
import java.util.StringJoiner;

import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.google.common.base.Preconditions;

public record ListStringPrinter(Printer elementPrinter, PrintSettings cfg, LocaleConfig.ListFormat listFormat) implements Printer {

	public ListStringPrinter(Printer elementPrinter, PrintSettings cfg) {
		this(elementPrinter, cfg, cfg.getListFormat());
	}

	@Override
	public String apply(Object f) {

		// Jackson deserializes collections as lists instead of an array, if the type is not given
		Preconditions.checkArgument(f instanceof List, "Expected a List got %s (as String `%s` )".formatted(f.getClass().getName(), f));

		final StringJoiner joiner = listFormat.createListJoiner();

		for (Object obj : (List<?>) f) {
			joiner.add(listFormat.escapeListElement(elementPrinter.apply(obj).toString()));
		}
		return joiner.toString();
	}
}
