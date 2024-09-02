package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.common.ToStringPrinter;
import lombok.ToString;

@ToString
public class JsonResultPrinters extends JavaResultPrinters {


	@Override
	public Printer getDatePrinter(PrintSettings printSettings) {
		return new ToStringPrinter(super.getDatePrinter(printSettings));
	}

	@Override
	public Printer getDateRangePrinter(PrintSettings printSettings) {
		return new ToStringPrinter(super.getDateRangePrinter(printSettings));
	}


}
