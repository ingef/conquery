package com.bakdata.conquery.models.query.resultinfo.printers;

import java.math.BigDecimal;

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
		return new MoneyPrinter(printSettings.getCurrency().getDefaultFractionDigits());
	}

	private record MoneyPrinter(int currencyFactionShift) implements Printer<Number> {

		@Override
		public Object apply(@NotNull Number value) {
			if (value instanceof BigDecimal bigDecimal){
				try{
					// In Arrow, we want to use the currency fractions as the base unit (e.g. cents instead of euros)
					return bigDecimal.movePointRight(currencyFactionShift).longValueExact();
				} catch (ArithmeticException e) {
					throw new IllegalArgumentException("Could not convert BigDecimal of %s (with fraction shift: %s) to currency fraction".formatted(value,currencyFactionShift), e);
				}
			}

			return value.longValue();
		}
	}
}
