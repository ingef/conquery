package com.bakdata.conquery.models.query.resultinfo.printers;

import java.nio.charset.Charset;
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

	private static final Charset MS1252 = Charset.forName("windows-1252");
	/**
	 * This is equivalent to `-∞`. For some reason ∞/infinity does not get rendered in WINDOWS-1252. This way however it works.
	 * The code-point is taken from <a href="https://www.ee.ucl.ac.uk/mflanaga/java/HTMLandASCIItableWin.html">this table</a>.
	 */
	public static final String NEGATIVE_INF_MS1252 = "-" + new String(new byte[]{0x22, 0x1E}, 0, 1, MS1252);
	/**
	 * This is equivalent to `+∞`. For some reason ∞/infinity does not get rendered in WINDOWS-1252. This way however it works.
	 * The code-point is taken from <a href="https://www.ee.ucl.ac.uk/mflanaga/java/HTMLandASCIItableWin.html">this table</a>.
	 */
	public static final String POSITIVE_INF_MS1252 = "+" + new String(new byte[]{0x22, 0x1E}, 0, 1, MS1252);


	public static final String POSITIVE_INF_DEFAULT = "+∞";
	public static final String NEGATIVE_INF_DEFAULT = "-∞";
	private final String negativeInf;
	private final String positiveInf;

	// Public for tests only.
	public StringResultPrinters(String negativeInf, String positiveInf) {
		this.negativeInf = negativeInf;
		this.positiveInf = positiveInf;
	}

	/**
	 * This circumvents a bug in the translation of infinity when using WINDOWS-1252 Charset.
	 */
	public static StringResultPrinters forCharset(Charset charset) {
		if (MS1252.equals(charset) || MS1252.contains(charset)) {
			return new StringResultPrinters(NEGATIVE_INF_MS1252, POSITIVE_INF_MS1252);
		}

		return new StringResultPrinters(NEGATIVE_INF_DEFAULT, POSITIVE_INF_DEFAULT);
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
