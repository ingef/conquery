package com.bakdata.conquery.models.query.resultinfo.printers;

import java.math.BigDecimal;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IdentityPrinter;
import org.jetbrains.annotations.NotNull;

public class ArrowResultPrinters extends JavaResultPrinters {

	@Override
	public Printer<? extends Number> getDatePrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

	@Override
	public Printer<BigDecimal> getMoneyPrinter(PrintSettings printSettings) {
		return new MoneyPrinter();
	}

	private record MoneyPrinter() implements Printer<BigDecimal> {
		@Override
		public Object apply(@NotNull BigDecimal value) {
			return value.unscaledValue().intValueExact();
		}
	}
}
