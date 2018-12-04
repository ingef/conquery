package com.bakdata.conquery.io.jackson;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class Jackson {
	public static final ObjectMapper MAPPER;
	public static final ObjectMapper BINARY_MAPPER;

	static {
		MAPPER = configure(io.dropwizard.jackson.Jackson.newObjectMapper());
		BINARY_MAPPER = configure(io.dropwizard.jackson.Jackson.newObjectMapper(new SmileFactory()));
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends ObjectMapper> T configure(T objectMapper) {
		SimpleModule serializers = new SimpleModule();
		
		return (T)objectMapper
				.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
				.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES)
				.enable(Feature.ALLOW_COMMENTS)
				.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
				.enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
				.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
				.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
				.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
				.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
				.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
				.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setLocale(Locale.ROOT)
				.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
				.registerModule(serializers)
				.registerModule(new JavaTimeModule())
				.registerModule(new ParameterNamesModule())
				.registerModule(new GuavaModule())
				.registerModule(new ConquerySerializersModule())
				.setSerializationInclusion(Include.NON_NULL)
				//.setAnnotationIntrospector(new RestrictingAnnotationIntrospector())
				.setInjectableValues(new MutableInjectableValues());
	}
}
