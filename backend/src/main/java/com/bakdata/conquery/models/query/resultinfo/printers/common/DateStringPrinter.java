package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.google.common.base.Preconditions;

public record DateStringPrinter(PrintSettings cfg) implements Printer {

	@Override
	public String apply(Object f) {
		Preconditions.checkArgument(f instanceof Number, "Expected an Number but got an '%s' with the value: %s".formatted(f.getClass().getName(), f));

		final Number number = (Number) f;
		return cfg.getDateFormatter().format(CDate.toLocalDate(number.intValue()));
	}
}
