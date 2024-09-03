package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record MoneyStringPrinter(PrintSettings cfg) implements Printer<Number> {

	@Override
	public String apply(Number f) {

		if (cfg.isPrettyPrint()) {
			return cfg.getCurrencyFormat().format(f);
		}

		return f.toString();
	}
}
