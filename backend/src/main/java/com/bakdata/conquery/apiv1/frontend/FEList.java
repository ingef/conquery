package com.bakdata.conquery.apiv1.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.EntityTag;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import lombok.Getter;

public class FEList implements JsonSerializable {

	private final List<Pair<ConceptElementId<?>, FENode>> content = new ArrayList<>();
	@Getter(lazy = true)
	private final EntityTag cacheId = new EntityTag(Integer.toString(content.hashCode()));
	

	public void add(ConceptElementId<?> id, FENode node) {
		content.add(Pair.of(id, node));
	}


	@Override
	public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeStartObject();
		for(Pair<ConceptElementId<?>, FENode> e: content) {
			gen.writeFieldName(e.getKey().toString());
			serializers.defaultSerializeValue(e.getValue(), gen);
		}
		gen.writeEndObject();
	}


	@Override
	public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		serialize(gen, serializers);
	}
}
