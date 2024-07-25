package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Optional;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class MetaIdReferenceDeserializer<ID extends Id<T>, T extends Identifiable<?>> extends JsonDeserializer<T> implements ContextualDeserializer {

	private Class<?> type;
	private JsonDeserializer<?> beanDeserializer;
	private Class<ID> idClass;
	private MetaStorage metaStorage;

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
			return (T) ctxt.handleUnexpectedToken(type, parser.getCurrentToken(), parser, "name references should be strings");
		}

		ID id = ctxt.readValue(parser, idClass);

		try {

			// Not all Components have registries, we leave it up to the validator to be angry.
			if (metaStorage == null) {
				return null;
			}

			T result = metaStorage.get(id);

			if (result == null) {
				throw new IdReferenceResolvingException(parser, "Could not find entry `" + id + "` of type " + type.getName(), id.toString(), type);
			}

			if (!type.isAssignableFrom(result.getClass())) {
				throw new InputMismatchException(String.format("Cannot assign type %s to %s ", result.getClass(), type));
			}

			return result;
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
		Class<ID> idClass = IdUtil.findIdClass(cl);

		final MetaStorage metaStorage = (MetaStorage) ctxt.findInjectableValue(MetaStorage.class.getName(), null, null);

		return new MetaIdReferenceDeserializer<>(cl, ctxt.getFactory().createBeanDeserializer(ctxt, type, descr), idClass, metaStorage);
	}

	@Override
	public SettableBeanProperty findBackReference(String refName) {
		return beanDeserializer.findBackReference(refName);
	}
}
