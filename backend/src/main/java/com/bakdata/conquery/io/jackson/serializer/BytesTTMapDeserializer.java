package com.bakdata.conquery.io.jackson.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.bakdata.conquery.util.dict.ABytesNode;
import com.bakdata.conquery.util.dict.BytesNode;
import com.bakdata.conquery.util.dict.BytesPatriciaNode;
import com.bakdata.conquery.util.dict.BytesPatriciaValueNode;
import com.bakdata.conquery.util.dict.BytesTTMap;
import com.bakdata.conquery.util.dict.BytesValueNode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;

public class BytesTTMapDeserializer extends JsonDeserializer<BytesTTMap> {

	@Override
	public BytesTTMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		expect(ctxt, p.currentToken(),	JsonToken.START_ARRAY);

		ABytesNode n = read(p,ctxt);
		expect(ctxt, p.nextToken(), JsonToken.END_ARRAY);
		
		if(n == null) {
			return new BytesTTMap();
		}
		return new BytesTTMap(n);
	}
	
	private ABytesNode read(JsonParser p, DeserializationContext ctxt) throws IOException {
		ABytesNode n;
		if(p.nextToken() == JsonToken.VALUE_NULL) {
			return null;
		}
		expect(ctxt, p.currentToken(), JsonToken.VALUE_NUMBER_INT);
		BitStore flags = Bits.asStore(new byte[] {p.getByteValue()});
		
		if(flags.getBit(1)) {
			expect(ctxt, p.nextToken(), JsonToken.VALUE_EMBEDDED_OBJECT);
			byte[] key = readBytes(p);
			if(flags.getBit(0)) {
				expect(ctxt, p.nextToken(), JsonToken.VALUE_NUMBER_INT);
				int value = p.getIntValue();
				n = new BytesPatriciaValueNode(key, value);
			}
			else {
				n = new BytesPatriciaNode(key);
			}
		}
		else {
			expect(ctxt, p.nextToken(), JsonToken.VALUE_NUMBER_INT);
			byte key = p.getByteValue();
			if(flags.getBit(0)) {
				expect(ctxt, p.nextToken(), JsonToken.VALUE_NUMBER_INT);
				int value = p.getIntValue();
				n = new BytesValueNode(key, value);
			}
			else {
				n = new BytesNode(key);
			}
		}
		
		if(flags.getBit(2)) {
			n.setLeft(read(p, ctxt));
		}
		if(flags.getBit(3)) {
			n.setMiddle(read(p, ctxt));
		}
		if(flags.getBit(4)) {
			n.setRight(read(p, ctxt));
		}
		
		return n;
	}

	private void expect(DeserializationContext ctxt, JsonToken token, JsonToken expected) throws JsonMappingException {
		if(token != expected) {
			ctxt.reportInputMismatch(BytesTTMap.class, "Expected "+expected+" but found "+token);
		}
	}
	
	private byte[] readBytes(JsonParser p) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		p.readBinaryValue(baos);
		return baos.toByteArray();
	}
}
