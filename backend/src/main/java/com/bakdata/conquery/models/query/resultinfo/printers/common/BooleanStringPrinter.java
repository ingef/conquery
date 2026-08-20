package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record BooleanStringPrinter(PrintSettings cfg, String trueVal, String falseVal) implements Printer<Boolean> {

	public static BooleanStringPrinter create(PrintSettings settings) {
		if (!settings.isPrettyPrint()) {
			return new BooleanStringPrinter(settings, "1", "0");
		}

		final Results localized = C10nCache.getLocalized(Results.class, settings.getLocale());
		return new BooleanStringPrinter(settings, localized.True(), localized.False());
	}


	@Override
	public String apply(@NotNull Boolean f) {
		return f ? trueVal : falseVal;
	}
}
