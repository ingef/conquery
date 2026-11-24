package com.bakdata.conquery.models.query.resultinfo.printers.common;

import c10n.C10N;
import com.bakdata.conquery.internationalization.DateContextResolutionC10n;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record ResolutionPrinter(PrintSettings cfg) implements Printer<String> {

	@Override
	public String apply(@NotNull String value) {
		try {
			final DateContextResolutionC10n c10n = C10N.get(DateContextResolutionC10n.class, cfg.getLocale());
			return switch (Resolution.valueOf(value)) {
				case COMPLETE -> c10n.complete();
				case YEARS -> c10n.year();
				case QUARTERS -> c10n.quarter();
				case DAYS -> c10n.day();
			};
		}
		catch (Exception e) {
			throw new IllegalArgumentException("%s is not a valid %s.".formatted(value, Resolution.class), e);
		}
	}
}
