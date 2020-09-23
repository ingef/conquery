package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Optional;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor
public class CBlockDeserializer extends JsonDeserializer<CBlock> implements ContextualDeserializer {

	private JsonDeserializer<CBlock> beanDeserializer;
	
	@Override
	public CBlock deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		CBlock block = beanDeserializer.deserialize(p, ctxt);
		
		Connector con = NamespaceCollection.get(ctxt).getOptional(block.getConnector()).get();
		Concept<?> concept = con.getConcept();
		if(concept instanceof TreeConcept && block.getMostSpecificChildren() != null) {
			TreeConcept tree = (TreeConcept) concept;

			// deduplicate concrete paths after loading from disk.
			for (int event = 0; event < block.getMostSpecificChildren().length; event++) {
				if (block.getMostSpecificChildren()[event] == null) {
					continue;
				}

				block.getMostSpecificChildren()[event] = tree.getElementByLocalId(block.getMostSpecificChildren()[event]).getPrefix();
			}
		}
		return block;
	}
	
	@Override
	public CBlock deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
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
		BeanDescription descr = ctxt.getConfig().introspect(type);
		JsonDeserializer<?> deser = ctxt.getFactory().createBeanDeserializer(ctxt, type, descr);
		if(deser instanceof ResolvableDeserializer) {
			((ResolvableDeserializer) deser).resolve(ctxt);
		}
		return new CBlockDeserializer((JsonDeserializer)deser);
	}
}
