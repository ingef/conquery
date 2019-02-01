package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.NamespaceCollection;
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

@AllArgsConstructor @NoArgsConstructor
public class NsIdReferenceDeserializer<ID extends NamespacedId&IId<T>, T extends Identifiable<?>> extends JsonDeserializer<T> implements ContextualDeserializer {

	private Class<?> type;
	private JsonDeserializer<?> beanDeserializer;
	private Parser<ID> idParser;
	
	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if(parser.getCurrentToken()==JsonToken.VALUE_STRING) {
			String text = parser.getText();
			try {
				ID id;
				
				//check if there was a dataset injected and if it is already a prefix
				Dataset dataset = (Dataset) ctxt.findInjectableValue(Dataset.class.getName(), null, null);
				if(dataset != null) {
					id = idParser.parsePrefixed(dataset.getName(), text);
				}
				else {
					id = idParser.parse(text);
				}
				
				Optional<T> result = NamespaceCollection.get(ctxt).getOptional(id);
				
				if(result.isPresent()) {
					return result.get();
				}
				else {
					throw new NoSuchElementException("Could not find entry "+id+" of type "+type);
				}
			} catch(Exception e) {
				throw new IllegalArgumentException("Could not find entry "+text+" of type "+type, e);
			}
		}
		else {
			return (T) ctxt.handleUnexpectedToken(type, parser.getCurrentToken(), parser, "name references should be strings");
		}
	}
	
	@Override
	public T deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
		return this.deserialize(p, ctxt);
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		JavaType type = Optional
				.ofNullable(ctxt.getContextualType())
				.orElseGet(property::getType);

		BeanDescription descr = ctxt.getConfig().introspect(type);
		
		while(type.isContainerType()) {
			type = type.getContentType();
		}
		Class<?> cl = type.getRawClass();
		Class<IId<?>> idClass = IId.findIdClass(cl);
		Parser<IId<Identifiable<?>>> parser = IId.<IId<Identifiable<?>>>createParser((Class)idClass);
		
		return new NsIdReferenceDeserializer(
				cl,
				ctxt.getFactory().createBeanDeserializer(ctxt, type, descr),
				parser
		);
		//.createContextual(ctxt, property)
	}
	
	@Override
	public SettableBeanProperty findBackReference(String refName) {
		return beanDeserializer.findBackReference(refName);
	}
}
