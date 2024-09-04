package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record IntegerStringPrinter(PrintSettings cfg) implements Printer<Number> {

	@Override
	public String apply(@NotNull Number f) {
		if (cfg.isPrettyPrint()) {
			return cfg.getIntegerFormat().format(f.longValue());
		}

		return f.toString();
	}
}
