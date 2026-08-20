package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record IdentityPrinter<T>() implements Printer<T> {

	@Override
	public Object apply(@NotNull T value) {
		return value;
	}
}
