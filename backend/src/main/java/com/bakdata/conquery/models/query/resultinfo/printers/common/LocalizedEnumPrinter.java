package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.common.LocalizedToString;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record LocalizedEnumPrinter<T extends Enum<T> & LocalizedToString>(PrintSettings cfg, Class<T> clazz) implements Printer<String> {
	@Override
	public String apply(@NotNull String f) {
		try {
			return Enum.valueOf(clazz, f).toString(cfg.getLocale());
		}
		catch (Exception e) {
			throw new IllegalArgumentException("%s is not a valid %s.".formatted(f, clazz), e);
		}
	}
}
