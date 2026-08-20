package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DateRangeStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DateStringPrinter;
import lombok.ToString;

/**
 * This class simply put's out native types where possible to let Jackson handle the Serialization.
 * Except for Date and DateRange, where the Frontend cannot ensure proper handling.
 */
@ToString
public class JsonResultPrinters extends JavaResultPrinters {

	@Override
	public Printer<Number> getDatePrinter(PrintSettings printSettings) {
		return new DateStringPrinter(printSettings);
	}

	@Override
	public Printer<List<Integer>> getDateRangePrinter(PrintSettings printSettings) {
		return new DateRangeStringPrinter(printSettings, StringResultPrinters.NEGATIVE_INF_DEFAULT, StringResultPrinters.POSITIVE_INF_DEFAULT);
	}


}
