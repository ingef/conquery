package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;

public abstract class PrinterFactory {
	public Printer printerFor(ResultType type, PrintSettings printSettings) {
		if (type instanceof ResultType.ListT<?> listT) {
			final Printer elementPrinter = printerFor(listT.getElementType(), printSettings);
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

	public abstract Printer getListPrinter(Printer elementPrinter, PrintSettings printSettings);

	public abstract Printer getBooleanPrinter(PrintSettings printSettings);

	public abstract Printer getIntegerPrinter(PrintSettings printSettings);

	public abstract Printer getNumericPrinter(PrintSettings printSettings);

	public abstract Printer getDatePrinter(PrintSettings printSettings);

	public abstract Printer getDateRangePrinter(PrintSettings printSettings);

	public abstract Printer getStringPrinter(PrintSettings printSettings);

	public abstract Printer getMoneyPrinter(PrintSettings printSettings);
}
