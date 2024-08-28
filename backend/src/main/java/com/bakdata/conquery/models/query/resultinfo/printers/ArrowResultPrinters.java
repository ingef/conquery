package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.query.PrintSettings;

public class ArrowResultPrinters extends JavaResultPrinters{

	@Override
	public Printer getDatePrinter(PrintSettings printSettings) {
		return getIntegerPrinter(printSettings);
	}

}
