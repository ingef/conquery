package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.io.OutputStream;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gc.iotools.stream.base.ExecutionModel;
import com.gc.iotools.stream.is.InputStreamFromOutputStream;

public class BlockDeserializer extends JsonDeserializer<Block> {

	private final static SerializedString FIELD_IMPORT = new SerializedString("import");
	private final static SerializedString FIELD_ENTITY = new SerializedString("entity");
	private final static SerializedString FIELD_CONTENT = new SerializedString("content");

	@Override
	public Block deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		if (!p.nextFieldName(FIELD_IMPORT)) {
			ctxt.handleUnexpectedToken(Block.class, p.currentToken(), p, "expected field 'import'");
		}
		Import imp = NamespaceCollection.get(ctxt).resolve(ImportId.Parser.INSTANCE.parse(p.nextTextValue()));
		
		if (!p.nextFieldName(FIELD_ENTITY)) {
			ctxt.handleUnexpectedToken(Block.class, p.currentToken(), p, "expected field 'entity'");
		}
		int entity = p.nextIntValue(-1);
		
		if (!p.nextFieldName(FIELD_CONTENT)) {
			ctxt.handleUnexpectedToken(Block.class, p.currentToken(), p, "expected field 'content'");
		}
		p.nextValue();

		try (InputStreamFromOutputStream<Void> isos = new InputStreamFromOutputStream<Void>(ExecutionModel.STATIC_THREAD_POOL) {
			@Override
			public Void produce(final OutputStream dataSink) throws Exception {
				p.readBinaryValue(dataSink);
				return null;
			}
		}) {
			return imp.getBlockFactory().readBlock(entity, imp, isos);
		}
	}
}
