package com.bakdata.conquery.io.jackson.mixin;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * (De-)Serialization Mixin for {@link Object2IntMap}.
 */
@JsonDeserialize(using = Object2IntMapMixIn.Deserializer.class)
@Slf4j
public class Object2IntMapMixIn {

	public static class Deserializer<K> extends StdDeserializer<Object2IntMap<K>> implements ContextualDeserializer {

		private final MapDeserializer mapDeserializer;

		public Deserializer() {
			super(Object2IntMap.class);
			this.mapDeserializer = null;
		}

		public Deserializer(MapDeserializer mapDeserializer) {
			super(Object2IntMap.class);
			this.mapDeserializer = mapDeserializer;
		}

		@Override
		public Object2IntMap<K> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
			final Object2IntOpenHashMap<K> map = new Object2IntOpenHashMap<>();
			Object2IntOpenHashMap map1 = map;
			return (Object2IntMap) mapDeserializer.deserialize(p, ctxt, (Map<Object, Object>) map1);
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {

			final JavaType mapType = ctxt.constructType(Object2IntMap.class);
			KeyDeserializer keyDeserializer;
			final JavaType keyType = ctxt.getContextualType().getKeyType();
			try {
				keyDeserializer = ctxt.findKeyDeserializer(keyType, property);
			}
			catch (InvalidDefinitionException e) {
				log.trace("Falling back to delegating key deserializer for type: {} ", keyType);
				final JsonDeserializer<Object> contextualKeyDeserializer = ctxt.findContextualValueDeserializer(keyType, property);
				keyDeserializer = StdKeyDeserializers.constructDelegatingKeyDeserializer(ctxt.getConfig(), keyType, contextualKeyDeserializer);
			}
			final JavaType valueType = ctxt.getContextualType().getContentType();
			final JsonDeserializer<Object> valueDeserializer = ctxt.findContextualValueDeserializer(valueType, property);
			final StdValueInstantiator valueInstantiator = new StdValueInstantiator(ctxt.getConfig(), valueType);
			return new Deserializer<>(new MapDeserializer(mapType, valueInstantiator, keyDeserializer, valueDeserializer, null));
		}
	}
}
