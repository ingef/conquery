package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.time.LocalDate;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record DatePrinter() implements Printer<Number> {

	@Override
	public LocalDate apply(@NotNull Number value) {
		return CDate.toLocalDate(value.intValue());
	}
}
