package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record DateStringPrinter(PrintSettings cfg) implements Printer<Number> {

	@Override
	public String apply(@NotNull Number f) {
		return cfg.getDateFormatter().format(CDate.toLocalDate(f.intValue()));
	}
}
