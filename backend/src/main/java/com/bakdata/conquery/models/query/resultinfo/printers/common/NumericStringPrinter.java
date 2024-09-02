package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record NumericStringPrinter(PrintSettings cfg) implements Printer {

	@Override
	public String apply(Object f) {
		if (cfg.isPrettyPrint()) {
			return cfg.getDecimalFormat().format(f);
		}

		return f.toString();
	}
}
