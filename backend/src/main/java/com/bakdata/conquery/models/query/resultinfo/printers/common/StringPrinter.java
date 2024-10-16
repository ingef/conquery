package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Objects;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record StringPrinter<T>() implements Printer<T> {
	@Override
	public String apply(@NotNull T f) {
		return Objects.toString(f);
	}
}
