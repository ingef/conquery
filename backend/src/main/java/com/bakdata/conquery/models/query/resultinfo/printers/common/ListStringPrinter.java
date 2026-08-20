package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;

import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record ListStringPrinter<T>(Printer<T> elementPrinter, PrintSettings cfg, LocaleConfig.ListFormat listFormat) implements Printer<Collection<T>> {

	public ListStringPrinter(Printer<T> elementPrinter, PrintSettings cfg) {
		this(elementPrinter, cfg, cfg.getListFormat());
	}

	@Override
	public String apply(@NotNull Collection<T> f) {

		final StringJoiner joiner = listFormat.createListJoiner();

		for (T obj : f) {
			if (obj == null){
				continue;
			}
			
			joiner.add(listFormat.escapeListElement(Objects.toString(elementPrinter.apply(obj))));
		}
		return joiner.toString();
	}
}
