package com.bakdata.conquery.models.i18n;

import java.util.Locale;

import c10n.C10N;
import c10n.C10NConfigBase;
import c10n.annotations.De;
import c10n.annotations.DefaultC10NAnnotations;
import c10n.annotations.En;


public final class I18n {
	// Used to trigger initialization of this class by the JVM in tests
	public final static I18n INSTANCE = new I18n();
	
	public static final ThreadLocal<Locale> LOCALE = ThreadLocal.withInitial(() -> Locale.ENGLISH);

	public static final Labels LABELS;
	static {
		C10N.configure(new C10NConfigBase() {
			@Override
			protected void configure() {
				install(new DefaultC10NAnnotations());
				bindAnnotation(En.class);
			}
		});
		LABELS = C10N.get(Labels.class);
	}

	public interface Labels {
		@En("Select used date")
		@De("Datum")
		String getDateSelection();
	}
}
