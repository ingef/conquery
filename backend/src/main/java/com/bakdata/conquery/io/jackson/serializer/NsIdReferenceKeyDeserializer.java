package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualKeyDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Deserializer for Map keys. See {@link NsIdRef} for details.
 */
@AllArgsConstructor
@NoArgsConstructor
public class NsIdReferenceKeyDeserializer<ID extends Id<VALUE> & NamespacedId, VALUE extends NamespacedIdentifiable<? extends ID>> extends KeyDeserializer implements ContextualKeyDeserializer {

	private IdUtil.Parser<ID> parser;

	@Override
	public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
		final ID id = parser.parse(key);

		return IdResolveContext.get(ctxt).<ID, VALUE>resolve(id);
	}

	@Override
	public KeyDeserializer createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {

		final Class<ID> idClass = IdUtil.findIdClass(property.getType().getKeyType().getRawClass());
		final IdUtil.Parser<ID> parser = IdUtil.createParser(idClass);


		return new NsIdReferenceKeyDeserializer<>(parser);
	}
}
