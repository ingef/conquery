package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DatePrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IdentityPrinter;
import com.bakdata.conquery.models.types.ResultType;

/**
 * This class is a mess because Excel supports some of our types natively.
 *
 * With LIST types we fall back onto the StringResultPrinter, as Excel does not support Lists.
 */
public class ExcelResultPrinters extends StringResultPrinters {

	private final PrinterFactory partialDelegate = new StringResultPrinters();

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
	public Printer<Number> getNumericPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

	@Override
	public Printer<Number> getMoneyPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

	@Override
	public Printer<Number> getDatePrinter(PrintSettings printSettings) {
		return new DatePrinter();
	}

	@Override
	public Printer<Number> getIntegerPrinter(PrintSettings printSettings) {
		return new IdentityPrinter<>();
	}

}
