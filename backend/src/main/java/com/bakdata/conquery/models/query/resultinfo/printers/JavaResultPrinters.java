package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DatePrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IdentityPrinter;
import org.jetbrains.annotations.NotNull;

public class JavaResultPrinters extends PrinterFactory {

	@Override
	public <T> Printer<List<T>> getListPrinter(Printer<T> elementPrinter, PrintSettings printSettings) {
		return new ListPrinter<>(elementPrinter);
	}

	@Override
	public Printer<Boolean> getBooleanPrinter(PrintSettings printSettings) {
		return new IdentityPrinter();
	}

	@Override
	public Printer<Number> getIntegerPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

	@Override
	public Printer<Number> getNumericPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

	@Override
	public Printer<Number> getDatePrinter(PrintSettings printSettings) {
		return new DatePrinter();
	}

	@Override
	public Printer<List<Integer>> getDateRangePrinter(PrintSettings printSettings) {
		return new DateRangePrinter();
	}

	@Override
	public Printer<String> getStringPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}


	@Override
	public Printer<Number> getMoneyPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

	private record ListPrinter<T>(Printer<T> elementPrinter) implements Printer<List<T>> {

		@Override
		public Object apply(@NotNull List<T> value) {
			final List<Object> out = new ArrayList<>(value.size());

			for (T elt : value) {
				out.add(elementPrinter.apply(elt));
			}

			return out;
		}
	}

	private record DateRangePrinter() implements Printer<List<Integer>> {

		@Override
		public Object apply(@NotNull List<Integer> f) {
			return CDateRange.of(f.get(0), f.get(1));
		}
	}
}
