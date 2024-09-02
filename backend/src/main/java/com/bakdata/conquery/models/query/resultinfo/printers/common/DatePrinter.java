package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record DatePrinter() implements Printer {

	@Override
	public Object apply(Object value) {
		return CDate.toLocalDate(((Number) value).intValue());
	}
}
