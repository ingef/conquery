package com.bakdata.conquery.models.query.resultinfo.printers.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.PrintSettings;
import org.junit.jupiter.api.Test;

class ResolutionPrinterTest {
	static {
		I18n.init();
	}

	@Test
	void shouldLocalizeResolutionNames() {
		assertEquals("complete", new ResolutionPrinter(settings(Locale.ENGLISH)).apply("COMPLETE"));
		assertEquals("Jahr", new ResolutionPrinter(settings(Locale.GERMAN)).apply("YEARS"));
		assertEquals("Quartal", new ResolutionPrinter(settings(Locale.GERMAN)).apply("QUARTERS"));
		assertEquals("day", new ResolutionPrinter(settings(Locale.ENGLISH)).apply("DAYS"));
	}

	@Test
	void shouldRejectUnknownResolutionNames() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new ResolutionPrinter(settings(Locale.ENGLISH)).apply("WEEKS")
		);
	}

	private static PrintSettings settings(Locale locale) {
		return new PrintSettings(true, locale, new ConqueryConfig(), null, null);
	}
}
