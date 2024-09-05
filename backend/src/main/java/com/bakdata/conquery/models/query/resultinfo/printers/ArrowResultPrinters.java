package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IdentityPrinter;
import org.jetbrains.annotations.NotNull;

public class ArrowResultPrinters extends JavaResultPrinters {

	@Override
	public Printer<Number> getDatePrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

	@Override
	public Printer<Number> getMoneyPrinter(PrintSettings printSettings) {
		return new MoneyPrinter();
	}

	private record MoneyPrinter() implements Printer<Number> {
		@Override
		public Object apply(@NotNull Number value) {
			return value.intValue();
		}
	}
}
