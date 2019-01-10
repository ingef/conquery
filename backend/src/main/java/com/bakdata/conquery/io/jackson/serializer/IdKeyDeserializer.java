package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualKeyDeserializer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor
public class IdKeyDeserializer<ID extends IId<?>> extends KeyDeserializer implements ContextualKeyDeserializer {

	private Class<ID> idClass;
	private Parser<ID> idParser;
	
	@Override
	public ID deserializeKey(String key, DeserializationContext ctxt) throws IOException {
		try {
			//check if there was a dataset injected and if it is already a prefix
			Dataset dataset = (Dataset) ctxt.findInjectableValue(Dataset.class.getName(), null, null);
			if(dataset != null) {
				return idParser.parsePrefixed(dataset.getName(), key);
			}
			else {
				return idParser.parse(key);
			}
		} catch(Exception e) {
			throw new IllegalArgumentException("Could not parse an "+idClass.getSimpleName()+" from "+key, e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public KeyDeserializer createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		JavaType type = Optional
				.ofNullable(ctxt.getContextualType())
				.orElseGet(Optional.ofNullable(property).map(BeanProperty::getType)::get);

		//since this a key serializer property should point to a map
		type = type.getKeyType();
		
		Class<IId<?>> idClass = (Class<IId<?>>) type.getRawClass();
		Parser<IId<Identifiable<?>>> parser = IId.<IId<Identifiable<?>>>createParser((Class)idClass);
		
		return new IdKeyDeserializer(idClass, parser);
	}
}
