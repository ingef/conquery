package com.bakdata.conquery.io.jackson.serializer;

import static com.bakdata.conquery.io.jackson.JacksonUtil.expect;

import java.io.IOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Data;

@JsonSerialize(using = Object2IntMapMixin.Serializer.class)
@JsonDeserialize(using = Object2IntMapMixin.Deserializer.class)
public class Object2IntMapMixin {

	public static class Serializer extends StdSerializer<Object2IntMap<?>> {

		protected Serializer() {
			super(Object2IntMap.class, true);
		}

		@Override
		public void serialize(Object2IntMap<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartArray();
			Object2IntMaps.fastForEach(value, new EntryWriter(gen));
			gen.writeEndArray();
		}

		private record EntryWriter(JsonGenerator gen) implements Consumer<Object2IntMap.Entry<?>> {
			@Override
			public void accept(Object2IntMap.Entry<?> entry) {
				try {
					gen.writeObject(new KeyValueObj<>(entry.getKey(), entry.getIntValue()));
				}
				catch (IOException exception) {
					throw new IllegalStateException(String.format("Unable to serialize map entry: k=%s, v=%d ", entry.getKey(), entry.getIntValue()));
				}
			}
		}
	}

	public static class Deserializer<K> extends StdDeserializer<Object2IntMap<?>> implements ContextualDeserializer {

		final JsonDeserializer<Object> keyDeserializer;

		/**
		 * Dummy for Jackson to call {@link Deserializer#createContextual(DeserializationContext, BeanProperty)}
		 */
		public Deserializer() {
			super(Object2IntMap.class);
			this.keyDeserializer = null;
		}

		public Deserializer(JsonDeserializer<Object> keyDeserializer) {
			super(Object2IntMap.class);
			this.keyDeserializer = keyDeserializer;
		}

		@Override
		public Object2IntMap<K> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
			expect(_valueClass, ctxt, p.currentToken(), JsonToken.START_ARRAY);

			Object2IntOpenHashMap<K> result = new Object2IntOpenHashMap<>();

			while (p.nextToken() != JsonToken.END_ARRAY) {
				// This cast should be fine
				KeyValueObj<K> entry = (KeyValueObj<K>) keyDeserializer.deserialize(p, ctxt);
				result.put(entry.getK(), entry.getV());
			}

			return result;
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
			// Get the parameter type of this map for the key
			final Class<?> keyType = ctxt.getContextualType().getKeyType().getRawClass();

			// Unfortunately we cannot use TypeReference here as in some cases Jackson would serialize a LinkHashMap instead of the actual object type
			final JavaType keyTypeRef = ctxt.getTypeFactory().constructParametricType(KeyValueObj.class, keyType);
			final JsonDeserializer<Object> keyDeserializer = ctxt.findRootValueDeserializer(keyTypeRef);

			return new Deserializer<>(keyDeserializer);
		}
	}


	/**
	 * Helper for serdes of key value pair
	 *
	 * @implNote don't convert to record, as jackson has no support for it yet
	 */
	@Data
	private static class KeyValueObj<K> {
		private final K k;
		private final int v;
	}
}
