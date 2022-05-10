package com.bakdata.conquery.io.jackson;

import java.util.Locale;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.experimental.UtilityClass;
import org.apache.shiro.authz.Permission;

@UtilityClass
public class Mappers {

	/**
	 * Helper method that also creates a copy of the injected values to reduce side effects.
	 * @param om the {@link ObjectMapper} which is copied. Its {@link com.fasterxml.jackson.databind.InjectableValues} must be {@link MutableInjectableValues}
	 * @return A copy of the {@link ObjectMapper} along with a copy of its {@link MutableInjectableValues}.
	 */
	public static ObjectMapper copyMapperAndInjectables(ObjectMapper om) {
		final ObjectMapper copy = om.copy();
		copy.setInjectableValues(((MutableInjectableValues)copy.getInjectableValues()).copy());
		return copy;
	}

	public static <T extends ObjectMapper> T configure(T objectMapper){

		objectMapper
			.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
			.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
			.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES)
			.enable(Feature.ALLOW_COMMENTS)
			.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
			//TODO this is just a hotfix to avoid reimports
//			.enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
			.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
			.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
			.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
			.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.setLocale(Locale.ROOT)
			.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
			.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
			.enable(SerializationFeature.WRITE_NULL_MAP_VALUES)
			.registerModule(new JavaTimeModule())
			.registerModule(new ParameterNamesModule())
			.registerModule(new GuavaModule())
			.registerModule(new AfterburnerModule())
			.registerModule(ConquerySerializersModule.INSTANCE)
			.setSerializationInclusion(Include.ALWAYS)
			.setDefaultPropertyInclusion(Include.ALWAYS)
			//.setAnnotationIntrospector(new RestrictingAnnotationIntrospector())
			.setInjectableValues(new MutableInjectableValues())
			.addMixIn(Permission.class, ConqueryPermission.class);

		objectMapper.setConfig(objectMapper.getSerializationConfig().withView(Object.class));
		return objectMapper;
	}

	public static <T> T findInjectable(DeserializationContext ctxt, Class<T> clazz) throws JsonMappingException {
		return (T) ctxt.findInjectableValue(clazz.getName(), null, null);
	}

	public static ObjectMapper getMapper() {
		return configure(io.dropwizard.jackson.Jackson.newObjectMapper());
	}

	public static ObjectMapper getBinaryMapper() {
		return configure(io.dropwizard.jackson.Jackson.newObjectMapper(new SmileFactory()));
	}
}
