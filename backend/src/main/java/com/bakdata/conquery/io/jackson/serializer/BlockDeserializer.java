package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class BlockDeserializer extends JsonDeserializer<Block> {

	private final static SerializedString FIELD_IMPORT = new SerializedString("import");
	private final static SerializedString FIELD_ENTITY = new SerializedString("entity");
	private final static SerializedString FIELD_CONTENT = new SerializedString("content");
	private final static Executor EXECUTORS = Executors.newCachedThreadPool(); 

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

		try(PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in = new PipedInputStream(out);) {
			EXECUTORS.execute(()->{
				try {
				p.readBinaryValue(out);
				out.close();
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			});
			return imp.getBlockFactory().readBlock(entity, imp, in);
		}
	}
}
