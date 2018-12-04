package com.bakdata.conquery.io.jackson.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BlockDeserializer extends JsonDeserializer<Block>{
	@Override
	public Block deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectNode json = p.readValueAs(ObjectNode.class);
		Import imp = NamespaceCollection.get(ctxt).resolve(ImportId.Parser.INSTANCE.parse(json.get("import").asText()));
		int entity = json.get("entity").asInt();
		return imp.getBlockFactory().readBlock(entity, imp, new ByteArrayInputStream(json.get("content").binaryValue()));
	}
}
