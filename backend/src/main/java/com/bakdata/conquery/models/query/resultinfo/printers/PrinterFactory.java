package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;

/**
 * This class allows {@link com.bakdata.conquery.models.datasets.concepts.select.Select}s to abstractly define printing, for all our renderers.
 *
 * The primary thing this class solves is {@link List} printing interacting with special handling like {@link com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect} and {@link com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect}.
 */
public abstract class PrinterFactory {
	/**
	 * Default implementation of determining the printer for a {@link ResultType}.
	 * Generally, this method should not be overriden and preferably be final, but {@link ExcelResultPrinters} makes this problematic.
	 */
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

	/**
	 * Jackson will opportunistically read {@link Long} and {@link Integer} hence our usage of Number.
	 */
	public abstract Printer<Number> getIntegerPrinter(PrintSettings printSettings);

	public abstract Printer<Number> getNumericPrinter(PrintSettings printSettings);

	/**
	 * Jackson will opportunistically read {@link Long} and {@link Integer} hence our usage of Number.
	 */
	public abstract Printer<Number> getDatePrinter(PrintSettings printSettings);

	public abstract Printer<List<Integer>> getDateRangePrinter(PrintSettings printSettings);

	public abstract Printer<String> getStringPrinter(PrintSettings printSettings);

	public abstract Printer<Number> getMoneyPrinter(PrintSettings printSettings);
}
