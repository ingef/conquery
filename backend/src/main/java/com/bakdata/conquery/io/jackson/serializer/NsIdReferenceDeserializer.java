package com.bakdata.conquery.io.jackson.serializer;

import static com.bakdata.conquery.io.jackson.serializer.SerdesTarget.MANAGER_AND_SHARD;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Optional;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.IdResolveContext;
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
public class NsIdReferenceDeserializer<ID extends Id<T> & NamespacedId, T extends Identifiable<?>> extends JsonDeserializer<T> implements ContextualDeserializer {

	private Class<?> type;
	private JsonDeserializer<?> beanDeserializer;
	private Class<ID> idClass;
	private SerdesTarget fieldTarget;

	@Override
	public T deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
		return this.deserialize(p, ctxt);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		final SerdesTarget omTarget = (SerdesTarget) ctxt.getAttribute(SerdesTarget.class);

		if (omTarget == null) {
			throw new IllegalStateException("Deserialization target not set. Don't know in whose context to deserialize.");
		}

		if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
			return (T) ctxt.handleUnexpectedToken(type, parser.getCurrentToken(), parser, "name references should be strings");
		}

		if (!omTarget.shouldDeserializeField(fieldTarget)) {
			log.trace("Skipping deserialization on instance (target={}) for field (target={}). Id was: {}", omTarget, fieldTarget, log.isTraceEnabled() ? parser.getText(): "trace not enabled");
			return null;
		}

		ID id = ctxt.readValue(parser, idClass);

		try {

			final IdResolveContext idResolveContext = IdResolveContext.get(ctxt);
			Optional<T> result = idResolveContext.getOptional(id);

			if (result.isEmpty()) {
				throw new IdReferenceResolvingException(parser, "Could not find entry `" + id + "` of type " + type.getName(), id.toString(), type);
			}

			if (!type.isAssignableFrom(result.get().getClass())) {
				throw new InputMismatchException(String.format("Cannot assign %s of type %s to %s ", id, result.get().getClass(), type));
			}

			return result.get();
		}
		catch (Exception e) {
			throw new RuntimeException("Error while resolving entry " + id + " of type " + type, e);
		}
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		JavaType type = Optional
								.ofNullable(ctxt.getContextualType())
								.orElseGet(property::getType);

		BeanDescription descr = ctxt.getConfig().introspect(type);

		while (type.isContainerType()) {
			type = type.getContentType();
		}
		Class<T> cl = (Class<T>) type.getRawClass();
		Class<ID> idClass = IdUtil.findIdClass(cl);

		// Get the serdes target, but don't evaluate it yet (however it might be possible to return a noop deserializer here)
		final NsIdRef annotation = property.getAnnotation(NsIdRef.class);
		final SerdesTarget serdesTarget = annotation != null ? annotation.serdesTarget() : MANAGER_AND_SHARD;

		return new NsIdReferenceDeserializer<>(
				cl,
				ctxt.getFactory().createBeanDeserializer(ctxt, type, descr),
				idClass,
				serdesTarget
		);
	}

	@Override
	public SettableBeanProperty findBackReference(String refName) {
		return beanDeserializer.findBackReference(refName);
	}
}
