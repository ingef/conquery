package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.BooleanStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DateRangeStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DateStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.ListStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.NumberFormatStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.StringPrinter;
import lombok.ToString;

/**
 * All printers in this factory should be assumed to return {@link String}, this is useful for CSV or HTML printing.
 */
@ToString
public class StringResultPrinters extends PrinterFactory {

	public static final String POSITIVE_INF = "+inf";
	public static final String NEGATIVE_INF = "-inf";
	private final String negativeInf;
	private final String positiveInf;

	public StringResultPrinters() {
		this(POSITIVE_INF, NEGATIVE_INF);
	}

	// Public for tests only.
	public StringResultPrinters(String negativeInf, String positiveInf) {
		this.negativeInf = negativeInf;
		this.positiveInf = positiveInf;
	}

	@Override
	public <T> Printer<Collection<T>> getListPrinter(Printer<T> elementPrinter, PrintSettings printSettings) {
		return new ListStringPrinter<>(elementPrinter, printSettings);
	}

	@Override
	public Printer<Boolean> getBooleanPrinter(PrintSettings printSettings) {
		return BooleanStringPrinter.create(printSettings);
	}

	@Override
	public Printer<Number> getIntegerPrinter(PrintSettings printSettings) {
		return NumberFormatStringPrinter.create(printSettings, printSettings.getIntegerFormat());
	}

	@Override
	public Printer<Number> getNumericPrinter(PrintSettings printSettings) {
		return NumberFormatStringPrinter.create(printSettings, printSettings.getDecimalFormat());
	}

	@Override
	public Printer<Number> getDatePrinter(PrintSettings printSettings) {
		return new DateStringPrinter(printSettings);
	}

	@Override
	public Printer<List<Integer>> getDateRangePrinter(PrintSettings printSettings) {
		return new DateRangeStringPrinter(printSettings, negativeInf, positiveInf);
	}

	@Override
	public Printer<String> getStringPrinter(PrintSettings printSettings) {
		return new StringPrinter();
	}

	@Override
	public Printer<Number> getMoneyPrinter(PrintSettings printSettings) {
		return NumberFormatStringPrinter.create(printSettings, printSettings.getCurrencyFormat());
	}

}
