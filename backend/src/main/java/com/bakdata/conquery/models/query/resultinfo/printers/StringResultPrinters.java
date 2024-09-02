package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.BooleanStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DateRangeStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.DateStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.IntegerStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.ListStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.MoneyStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.NumericStringPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.StringPrinter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringResultPrinters extends PrinterFactory {


	@Override
	public Printer getListPrinter(Printer elementPrinter, PrintSettings printSettings) {
		return new ListStringPrinter(elementPrinter, printSettings);
	}

	@Override
	public Printer getBooleanPrinter(PrintSettings printSettings) {
		return BooleanStringPrinter.create(printSettings);
	}

	@Override
	public Printer getIntegerPrinter(PrintSettings printSettings) {
		return new IntegerStringPrinter(printSettings);
	}

	@Override
	public Printer getNumericPrinter(PrintSettings printSettings) {
		return new NumericStringPrinter(printSettings);
	}

	@Override
	public Printer getDatePrinter(PrintSettings printSettings) {
		return new DateStringPrinter(printSettings);
	}

	@Override
	public Printer getDateRangePrinter(PrintSettings printSettings) {
		return new DateRangeStringPrinter(printSettings);
	}

	@Override
	public Printer getStringPrinter(PrintSettings printSettings) {
		return new StringPrinter();
	}

	@Override
	public Printer getMoneyPrinter(PrintSettings printSettings) {
		return new MoneyStringPrinter(printSettings);
	}

}
