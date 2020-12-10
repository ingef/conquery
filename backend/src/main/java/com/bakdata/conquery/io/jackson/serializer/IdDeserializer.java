package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor
public class IdDeserializer<ID extends IId<?>> extends JsonDeserializer<ID> implements ContextualDeserializer {

	private Class<ID> idClass;
	private Parser<ID> idParser;
	private boolean checkForInjectedPrefix;
	
	@SuppressWarnings("unchecked")
	@Override
	public ID deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if(parser.getCurrentToken()==JsonToken.VALUE_STRING) {
			String text = parser.getText();
			try {
				if(checkForInjectedPrefix) {
					//check if there was a dataset injected and if it is already a prefix
					Dataset dataset = (Dataset) ctxt.findInjectableValue(Dataset.class.getName(), null, null);
					if(dataset != null) {
						return idParser.parsePrefixed(dataset.getName(), text);
					}
				}
				return idParser.parse(text);
			} catch(Exception e) {
				throw new IllegalArgumentException("Could not parse an "+idClass.getSimpleName()+" from "+text, e);
			}
		}
		return (ID) ctxt.handleUnexpectedToken(IId.class, parser.getCurrentToken(), parser, "name references should be strings");
	}
	
	@Override
	public ID deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
		return this.deserialize(p, ctxt);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		JavaType type = Optional
				.ofNullable(ctxt.getContextualType())
				.orElseGet(Optional.ofNullable(property).map(BeanProperty::getType)::get);

		while(type.isContainerType()) {
			type = type.getContentType();
		}
		Class<IId<?>> idClass = (Class<IId<?>>) type.getRawClass();
		Parser<IId<Identifiable<?>>> parser = IId.<IId<Identifiable<?>>>createParser((Class)idClass);
		
		return new IdDeserializer(
			idClass,
			parser,
			//we only need to check for the dataset prefix if the id requires it
			NamespacedId.class.isAssignableFrom(idClass)
		);
	}
}
