package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Objects;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record StringPrinter() implements Printer<String> {
	@Override
	public String apply(@NotNull String f) {
		return Objects.toString(f);
	}
}
