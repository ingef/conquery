package com.bakdata.conquery.models.query.resultinfo.printers;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IdentityPrinter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
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
			if (value instanceof BigDecimal bigDecimal){
				if (bigDecimal.scale() > 2) {
					log.warn("Rounding money value '{}' as scale is greater than 2", bigDecimal);
				}
				return bigDecimal.setScale(2, RoundingMode.HALF_EVEN).unscaledValue().intValueExact();
			}

			return value.intValue();
		}
	}
}
