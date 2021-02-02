package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Optional;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
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
				
				String datasetName = null;
				//check if there was a dataset injected and if it is already a prefix
				datasetName = Optional.ofNullable(Jackson.findInjectable(ctxt, Dataset.class)).map(Dataset::getName).orElse(null);
				//maybe only the id was injected
				if(datasetName == null) {
					datasetName = Optional.ofNullable(Jackson.findInjectable(ctxt, DatasetId.class)).map(DatasetId::getName).orElse(null);
				}
				
				if(datasetName != null) {
					id = idParser.parsePrefixed(datasetName, text);
				}
				else {
					id = idParser.parse(text);
				}

				final IdResolveContext idResolveContext = IdResolveContext.get(ctxt);
				Optional<T> result = idResolveContext.getOptional(id);

				if (!result.isPresent()) {
					throw new IdReferenceResolvingException(parser, "Could not find entry `"+id+"` of type "+type.getName(), text, type);
				}

				if(!type.isAssignableFrom(result.get().getClass())) {
					throw new InputMismatchException(String.format("Cannot assign %s of type %s to %s ", id, result.get().getClass(), type));
				}

				return result.get();
			}
			catch(Exception e) {
				throw new RuntimeException("Error while resolving entry "+text+" of type "+type, e);
			}
		}
		return (T) ctxt.handleUnexpectedToken(type, parser.getCurrentToken(), parser, "name references should be strings");
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
