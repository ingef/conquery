package com.bakdata.conquery.io.freemarker;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.bakdata.conquery.models.events.generation.ClassGenerator;

import freemarker.template.Configuration;

public class Freemarker {

	public static Configuration createForJavaTemplates() {
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
		cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setLocalizedLookup(false);
		cfg.setClassForTemplateLoading(ClassGenerator.class, "/");
		cfg.setWhitespaceStripping(true);
		cfg.setNumberFormat("computer");
		cfg.setLocale(Locale.ROOT);
		return cfg;
	}
}
