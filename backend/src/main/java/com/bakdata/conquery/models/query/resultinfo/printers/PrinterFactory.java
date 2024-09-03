package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;


public abstract class PrinterFactory {
	public <T> Printer<T> printerFor(ResultType type, PrintSettings printSettings) {
		if (type instanceof ResultType.ListT<?> listT) {
			final Printer<?> elementPrinter = printerFor(listT.getElementType(), printSettings);
			return (Printer<T>) getListPrinter(elementPrinter, printSettings);
		}

		return (Printer<T>) switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> getBooleanPrinter(printSettings);
			case INTEGER -> getIntegerPrinter(printSettings);
			case NUMERIC -> getNumericPrinter(printSettings);
			case DATE -> getDatePrinter(printSettings);
			case DATE_RANGE -> getDateRangePrinter(printSettings);
			case STRING -> getStringPrinter(printSettings);
			case MONEY -> getMoneyPrinter(printSettings);
		};
	}

	public abstract <T> Printer<List<T>> getListPrinter(Printer<T> elementPrinter, PrintSettings printSettings);

	public abstract Printer<Boolean> getBooleanPrinter(PrintSettings printSettings);

	public abstract Printer<Number> getIntegerPrinter(PrintSettings printSettings);

	public abstract Printer<Number> getNumericPrinter(PrintSettings printSettings);

	public abstract Printer<Number> getDatePrinter(PrintSettings printSettings);

	public abstract Printer<List<Integer>> getDateRangePrinter(PrintSettings printSettings);

	public abstract Printer<String> getStringPrinter(PrintSettings printSettings);

	public abstract Printer<Number> getMoneyPrinter(PrintSettings printSettings);
}
