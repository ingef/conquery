package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Optional;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class MetaIdReferenceDeserializer<ID extends IId<T>, T extends Identifiable<?>> extends JsonDeserializer<T> implements ContextualDeserializer {

	private Class<?> type;
	private JsonDeserializer<?> beanDeserializer;
	private Class<ID> idClass;

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
			return (T) ctxt.handleUnexpectedToken(type, parser.getCurrentToken(), parser, "name references should be strings");
		}

		ID id = ctxt.readValue(parser, idClass);

		try {
			final CentralRegistry centralRegistry = CentralRegistry.get(ctxt);

			// Not all Components have registries, we leave it up to the validator to be angry.
			if (centralRegistry == null) {
				return null;
			}

			Optional<T> result = centralRegistry.getOptional(id);

			if (result.isEmpty()) {
				throw new IdReferenceResolvingException(parser, "Could not find entry `" + id + "` of type " + type.getName(), id.toString(), type);
			}

			if (!type.isAssignableFrom(result.get().getClass())) {
				throw new InputMismatchException(String.format("Cannot assign type %s to %s ", result.get().getClass(), type));
			}

			return result.get();
		}
		catch (Exception e) {
			log.error("Error while resolving entry {} of type {}", id, type, e);
			throw e;
		}
	}

	@Override
	public T deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
		return this.deserialize(p, ctxt);
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {

		JavaType type = Optional.ofNullable(ctxt.getContextualType())
								.orElseGet(property::getType);

		BeanDescription descr = ctxt.getConfig().introspect(type);

		while (type.isContainerType()) {
			type = type.getContentType();
		}

		Class<?> cl = type.getRawClass();
		Class<ID> idClass = IId.findIdClass(cl);

		return new MetaIdReferenceDeserializer<>(cl, ctxt.getFactory().createBeanDeserializer(ctxt, type, descr), idClass);
	}

	@Override
	public SettableBeanProperty findBackReference(String refName) {
		return beanDeserializer.findBackReference(refName);
	}
}
