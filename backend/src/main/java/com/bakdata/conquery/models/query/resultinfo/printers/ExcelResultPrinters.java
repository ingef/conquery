package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DatePrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IdentityPrinter;
import com.bakdata.conquery.models.types.ResultType;

/**
 * This class is a mess because Excel supports some of our types natively.
 * <p>
 * With LIST types we fall back onto the StringResultPrinter, as Excel does not support Lists, BUT we also cannot use the {@link IdentityPrinter} inside the list, as some of our printers are native types.
 */
public class ExcelResultPrinters extends StringResultPrinters {

	public static final String NEGATIVE_INF = "-∞";
	public static final String POSITIVE_INF = "+∞";
	private final PrinterFactory partialDelegate = new StringResultPrinters(NEGATIVE_INF, POSITIVE_INF);

	public ExcelResultPrinters() {
		super(NEGATIVE_INF, POSITIVE_INF);
	}

	public Printer<?> printerFor(ResultType type, PrintSettings printSettings) {
		if (type instanceof ResultType.ListT<?> listT) {
			final Printer elementPrinter = partialDelegate.printerFor(listT.getElementType(), printSettings);
			return getListPrinter(elementPrinter, printSettings);
		}

		return switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> getBooleanPrinter(printSettings);
			case INTEGER -> getIntegerPrinter(printSettings);
			case NUMERIC -> getNumericPrinter(printSettings);
			case DATE -> getDatePrinter(printSettings);
			case DATE_RANGE -> getDateRangePrinter(printSettings);
			case STRING -> getStringPrinter(printSettings);
			case MONEY -> getMoneyPrinter(printSettings);
		};
	}

	@Override
	public Printer<Boolean> getBooleanPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
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
	public Printer<Number> getMoneyPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}
}
