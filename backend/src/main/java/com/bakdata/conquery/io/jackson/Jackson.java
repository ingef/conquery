package com.bakdata.conquery.io.jackson;

import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSetter.Value;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sun.xml.bind.v2.schemagen.xmlschema.List;

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
		
		objectMapper
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
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.setLocale(Locale.ROOT)
			.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
			.registerModule(serializers)
			.registerModule(new JavaTimeModule())
			.registerModule(new ParameterNamesModule())
			.registerModule(new GuavaModule())
			.registerModule(ConquerySerializersModule.INSTANCE)
			.setSerializationInclusion(Include.NON_NULL)
			//.setAnnotationIntrospector(new RestrictingAnnotationIntrospector())
			.setInjectableValues(new MutableInjectableValues());
		
		objectMapper
			.configOverride(List.class)
			.setSetterInfo(Value.forValueNulls(Nulls.AS_EMPTY));
		objectMapper
			.configOverride(Set.class)
			.setSetterInfo(Value.forValueNulls(Nulls.AS_EMPTY));
		
		return (T)objectMapper;
	}
	
	public static <T> T findInjectable(DeserializationContext ctxt, Class<T> clazz) throws JsonMappingException {
		return (T) ctxt.findInjectableValue(clazz.getName(), null, null);
	}
}
