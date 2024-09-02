package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DatePrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IdentityPrinter;
import com.google.common.base.Preconditions;

public class JavaResultPrinters extends PrinterFactory {

	@Override
	public Printer getListPrinter(Printer elementPrinter, PrintSettings printSettings) {
		return new ListPrinter(elementPrinter);
	}

	@Override
	public Printer getBooleanPrinter(PrintSettings printSettings) {
		return new IdentityPrinter();
	}

	@Override
	public Printer getIntegerPrinter(PrintSettings printSettings) {
		return new IdentityPrinter();
	}

	@Override
	public Printer getNumericPrinter(PrintSettings printSettings) {
		return new IdentityPrinter();
	}

	@Override
	public Printer getDatePrinter(PrintSettings printSettings) {
		return new DatePrinter();
	}

	@Override
	public Printer getDateRangePrinter(PrintSettings printSettings) {
		return new DateRangePrinter();
	}

	@Override
	public Printer getStringPrinter(PrintSettings printSettings) {
		return new IdentityPrinter();
	}


	@Override
	public Printer getMoneyPrinter(PrintSettings printSettings) {
		return new IdentityPrinter();
	}

	private record ListPrinter(Printer elementPrinter) implements Printer {

		@Override
		public Object apply(Object value) {
			final List<?> inList = (List) value;
			final List<Object> out = new ArrayList<>(inList.size());

			for (Object elt : inList) {
				out.add(elementPrinter.apply(elt));
			}

			return out;
		}
	}

	private record DateRangePrinter() implements Printer {


		@Override
		public Object apply(Object f) {
			Preconditions.checkArgument(f instanceof List<?>, "Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f);
			Preconditions.checkArgument(((List<?>) f).size() == 2, "Expected a list with 2 elements, one min, one max. The list was: %s ", f);

			final List<?> list = (List<?>) f;
			final Integer min = (Integer) list.get(0);
			final Integer max = (Integer) list.get(1);


			return CDateRange.of(min, max);
		}
	}
}
