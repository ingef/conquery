package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.common.LocalizedToString;
import com.bakdata.conquery.models.query.PrintSettings;

public record LocalizedEnumPrinter<T extends Enum<T> & LocalizedToString>(PrintSettings cfg, Class<T> clazz) implements Printer {
	@Override
	public String print(Object f) {

		if (clazz.isInstance(f)) {
			return clazz.cast(f).toString(cfg.getLocale());
		}
		try {
			return Enum.valueOf(clazz, f.toString()).toString(cfg.getLocale());
		}
		catch (Exception e) {
			throw new IllegalArgumentException("%s is not a valid %s.".formatted(f, clazz), e);
		}
	}
}
