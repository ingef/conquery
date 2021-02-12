package com.bakdata.conquery.io.freemarker;

import static freemarker.core.Configurable.*;
import static freemarker.template.Configuration.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import freemarker.template.Version;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Freemarker {
	
	public static final Version VERSION = Configuration.VERSION_2_3_27;
	
	public static final FreemarkerViewRenderer HTML_RENDERER = rendererForHtml();


	private static FreemarkerViewRenderer rendererForHtml() {
		FreemarkerViewRenderer freemarker = new FreemarkerViewRenderer(VERSION);
		freemarker.configure(Freemarker.asMap());
		return freemarker;
	}
	
	private static Map<String, String> asMap() {
		return ImmutableMap
			.<String, String>builder()
			.put(DEFAULT_ENCODING_KEY_CAMEL_CASE, StandardCharsets.UTF_8.name())
			.put(LOCALIZED_LOOKUP_KEY_CAMEL_CASE, Boolean.TRUE.toString())
			.put(WHITESPACE_STRIPPING_KEY_CAMEL_CASE, Boolean.TRUE.toString())
			.put(WRAP_UNCHECKED_EXCEPTIONS_KEY_CAMEL_CASE, Boolean.TRUE.toString())
			.put(LOG_TEMPLATE_EXCEPTIONS_KEY_CAMEL_CASE, Boolean.FALSE.toString())
			.put(NUMBER_FORMAT_KEY_CAMEL_CASE, "computer")
			.put(LOCALE_KEY, "")
			.build();
	}
}
