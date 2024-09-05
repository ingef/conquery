package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.math.BigDecimal;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record MoneyStringPrinter(PrintSettings cfg) implements Printer<BigDecimal> {

	@Override
	public String apply(@NotNull BigDecimal f) {

		if (cfg.isPrettyPrint()) {
			return cfg.getCurrencyFormat().format(f);
		}

		return f.toString();
	}
}
