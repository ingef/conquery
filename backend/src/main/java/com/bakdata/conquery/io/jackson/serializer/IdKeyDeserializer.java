package com.bakdata.conquery.io.jackson.serializer;

import java.util.Optional;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualKeyDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class IdKeyDeserializer<ID extends AId<?>> extends KeyDeserializer implements ContextualKeyDeserializer {

	private Class<ID> idClass;
	private Parser<ID> idParser;
	private boolean parsePrefixed;

	@Override
	public ID deserializeKey(String key, DeserializationContext ctxt) {
		try {
			return IdDeserializer.deserializeId(key, idParser, parsePrefixed, ctxt);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Could not parse an " + idClass.getSimpleName() + " from " + key, e);
		}
	}

	@Override
	public KeyDeserializer createContextual(DeserializationContext ctxt, BeanProperty property) {
		JavaType type = Optional.ofNullable(ctxt.getContextualType())
								.orElseGet(Optional.ofNullable(property).map(BeanProperty::getType)::get);

		//since this a key serializer property should point to a map
		type = type.getKeyType();

		Class<ID> idClass = (Class<ID>) type.getRawClass();
		Parser<ID> parser = IdUtil.createParser(idClass);

		return new IdKeyDeserializer<>(idClass, parser, NamespacedId.class.isAssignableFrom(idClass));
	}
}
