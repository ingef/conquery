package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.ArrayDeque;

import com.bakdata.conquery.util.dict.ABytesNode;
import com.bakdata.conquery.util.dict.BytesNode;
import com.bakdata.conquery.util.dict.BytesPatriciaNode;
import com.bakdata.conquery.util.dict.BytesTTMap;
import com.bakdata.conquery.util.dict.ValueNode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;

public class BytesTTMapSerializer extends JsonSerializer<BytesTTMap> {

	@Override
	public void serialize(BytesTTMap value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			
		gen.writeStartArray();
		
		if(value.getRoot() == null) {
			gen.writeNull();
		}
		else {
			byte[] bytes = new byte[1];
			BitStore flags = Bits.asStore(bytes);
			
					
			//second run write the data collect the flags
			ArrayDeque<ABytesNode> openList = new ArrayDeque<>();
			openList.add(value.getRoot());
			while(!openList.isEmpty()) {
				ABytesNode n = openList.removeFirst();
				flags.clear();
				flags.setBit(0, n instanceof ValueNode);
				flags.setBit(1, n instanceof BytesPatriciaNode);
				flags.setBit(2, n.getLeft() != null);
				flags.setBit(3, n.getMiddle() != null);
				flags.setBit(4, n.getRight() != null);
				gen.writeNumber(bytes[0]);
				
				
				if(n instanceof BytesPatriciaNode) {
					gen.writeBinary(n.key());
				}
				else {
					gen.writeNumber(((BytesNode)n).getKey());
				}
				if(n instanceof ValueNode) {
					gen.writeNumber(((ValueNode) n).getValue());
				}
				
				//reverse because of addFirst
				if(n.getRight() != null) {
					openList.addFirst(n.getRight());
				}
				if(n.getMiddle() != null) {
					openList.addFirst(n.getMiddle());
				}
				if(n.getLeft() != null) {
					openList.addFirst(n.getLeft());
				}
			}
		}
		
		gen.writeEndArray();
	}
}
