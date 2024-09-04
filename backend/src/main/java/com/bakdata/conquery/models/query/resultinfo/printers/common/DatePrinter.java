package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record DatePrinter() implements Printer<Number> {

	@Override
	public Object apply(@NotNull Number value) {
		return CDate.toLocalDate(value.intValue());
	}
}
