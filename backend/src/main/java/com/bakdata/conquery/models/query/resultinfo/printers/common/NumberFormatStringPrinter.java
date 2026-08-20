package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.text.NumberFormat;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record NumberFormatStringPrinter(NumberFormat format) implements Printer<Number> {

	public static Printer<Number> create(PrintSettings cfg, NumberFormat currencyFormat) {
		if (cfg.isPrettyPrint()) {
			return new NumberFormatStringPrinter(currencyFormat);
		}
		return new StringPrinter<>();
	}

	@Override
	public String apply(@NotNull Number f) {
		return format.format(f);
	}
}
