package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.esotericsoftware.kryo.io.Input;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

public class BucketDeserializer extends JsonDeserializer<Bucket> {

	private final static SerializedString FIELD_BUCKET = new SerializedString(Bucket.Fields.bucket);
	private final static SerializedString FIELD_IMPORT = new SerializedString(Bucket.Fields.imp);
	private final static SerializedString FIELD_NUMBER_OF_EVENTS = new SerializedString(Bucket.Fields.numberOfEvents);
	private final static SerializedString FIELD_OFFSET = new SerializedString(Bucket.Fields.offsets);
	private final static SerializedString FIELD_CONTENT = new SerializedString("content");
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
		Import imp = IdResolveContext.get(ctxt).resolve(ImportId.Parser.INSTANCE.parse(p.nextTextValue()));
		
		if (!p.nextFieldName(FIELD_NUMBER_OF_EVENTS)) {
			ctxt.handleUnexpectedToken(Bucket.class, p.currentToken(), p, "expected field 'numberOfEvents'");
		}
		int numberOfEvents = p.nextIntValue(0);
		
		if (!p.nextFieldName(FIELD_OFFSET)) {
			ctxt.handleUnexpectedToken(Bucket.class, p.currentToken(), p, "expected field 'offsets'");
		}
		p.nextValue();
		int[] offsets = p.readValueAs(int[].class);
		
		Bucket bucket = imp.getBlockFactory().construct(bucketNumber, imp, offsets);
		
		if (!p.nextFieldName(FIELD_CONTENT)) {
			ctxt.handleUnexpectedToken(Bucket.class, p.currentToken(), p, "expected field 'content'");
		}
		p.nextValue();
		try(PipedOutputStream out = new PipedOutputStream();
			Input in = new Input(new PipedInputStream(out));) {
			EXECUTORS.execute(()->{
				try {
				p.readBinaryValue(out);
				out.close();
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			});
			bucket.read(in);
		}
		return bucket;
	}
}
