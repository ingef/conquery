package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Optional;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
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
@AllArgsConstructor @NoArgsConstructor
public class MetaIdReferenceDeserializer<ID extends IId<T>, T extends Identifiable<?>> extends JsonDeserializer<T> implements ContextualDeserializer {

	private Class<?> type;
	private JsonDeserializer<?> beanDeserializer;
	private Parser<ID> idParser;
	
	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if(parser.getCurrentToken()==JsonToken.VALUE_STRING) {
			String text = parser.getText();
			try {
				Optional<T> result = CentralRegistry.get(ctxt).getOptional(idParser.parse(text));

				if (!result.isPresent()) {
					return (T) ctxt.handleWeirdStringValue(type, text, "Could not find entry "+text+" of type "+type.getName());
				}

				if(!type.isAssignableFrom(result.get().getClass())) {
					throw new InputMismatchException(String.format("Cannot assign type %s to %s ", result.get().getClass(), type));
				}

				return result.get();
			} catch(Exception e) {
				log.error("Error while resolving entry "+text+" of type "+type, e);
				throw e;
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
		
		if(NamespacedId.class.isAssignableFrom(idClass)) {
			throw new IllegalStateException("@MetaIdRef should only be used for non NamespacedId fields");
		}
		
		Parser<IId<Identifiable<?>>> parser = IId.<IId<Identifiable<?>>>createParser((Class)idClass);
		
		return new MetaIdReferenceDeserializer(
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
