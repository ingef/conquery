package com.bakdata.conquery.io.jackson.serializer;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.CBlock;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@AllArgsConstructor @NoArgsConstructor
public class CBlockDeserializer extends JsonDeserializer<CBlock> implements ContextualDeserializer {

	private JsonDeserializer<CBlock> beanDeserializer;
	
	@Override
	public CBlock deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		CBlock block = beanDeserializer.deserialize(p, ctxt);

		TreeConcept concept = block.getConnector().getConcept();

		if(concept != null && block.getMostSpecificChildren() != null) {

			// deduplicate concrete paths after loading from disk.
			for (int event = 0; event < block.getMostSpecificChildren().length; event++) {
				int[] mostSpecificChildren = block.getMostSpecificChildren()[event];

				if (mostSpecificChildren == null || Connector.isNotContained(mostSpecificChildren)) {
					block.getMostSpecificChildren()[event] = Connector.NOT_CONTAINED;
					continue;
				}

				log.trace("Getting Elements for local ids: {}", mostSpecificChildren);
				block.getMostSpecificChildren()[event] = concept.getElementByLocalId(mostSpecificChildren).getPrefix();
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
