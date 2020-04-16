package com.bakdata.conquery.models.i18n;

import java.util.Locale;

import c10n.C10N;
import c10n.C10NConfigBase;
import c10n.annotations.DefaultC10NAnnotations;
import c10n.annotations.En;

public final class I18n {

	public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

	public static final ThreadLocal<Locale> LOCALE = ThreadLocal.withInitial(() -> Locale.ENGLISH);

	public static void init() {
		C10N.configure(new C10NConfigBase() {

			@Override
			protected void configure() {
				install(new DefaultC10NAnnotations());
				// Set English as default/fallback locale
				bindAnnotation(En.class);
			}
		});
	}
}
