package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class BucketDeserializer extends JsonDeserializer<Bucket> {

	private final static SerializedString FIELD_IMPORT = new SerializedString("imp");
	private final static SerializedString FIELD_BUCKET = new SerializedString("bucket");
	private final static SerializedString FIELD_BLOCKS = new SerializedString("blocks");
	private final static Executor EXECUTORS = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().namingPattern("BucketDeserializer %s").build()); 

	@Override
	public Bucket deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		if (!p.nextFieldName(FIELD_BUCKET)) {
			ctxt.handleUnexpectedToken(Bucket.class, p.currentToken(), p, "expected field 'bucket'");
		}
		int bucketNumber = p.nextIntValue(-1);
		
		if (!p.nextFieldName(FIELD_IMPORT)) {
			ctxt.handleUnexpectedToken(Bucket.class, p.currentToken(), p, "expected field 'imp'");
		}
		Import imp = NamespaceCollection.get(ctxt).resolve(ImportId.Parser.INSTANCE.parse(p.nextTextValue()));
		
		if (!p.nextFieldName(FIELD_BLOCKS)) {
			ctxt.handleUnexpectedToken(Bucket.class, p.currentToken(), p, "expected field 'blocks'");
		}
		
		List<Block> blocks = new ArrayList<>();
		if (p.nextToken() != JsonToken.START_ARRAY) {
			ctxt.handleUnexpectedToken(Bucket.class, p.currentToken(), p, "expected array start");
		}
		while(p.nextToken() != JsonToken.END_ARRAY) {
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
				blocks.add(imp.getBlockFactory().readBlock(imp, in));
			}
		}
		
		Bucket bucket = new Bucket();
		bucket.setBlocks(blocks.toArray(new Block[0]));
		bucket.setBucket(bucketNumber);
		bucket.setImp(imp);
		for(Block block:bucket.getBlocks()) {
			block.setBucket(bucket);
		}
		return bucket;
	}
}
