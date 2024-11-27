package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Objects;

import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record ToStringPrinter<T>(Printer<T> delegate) implements Printer<T> {

	@Override
	public Object apply(@NotNull T value) {
		return Objects.toString(delegate.apply(value));
	}
}
