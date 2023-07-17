package com.bakdata.conquery.util.dict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;


public interface ValueNode {

	public ABytesNode getParent();
	public int getValue();

	public record Entry(int id, String value){}

	public default Entry toEntry() {
		return new Entry(this.getValue(), toValue());
	}

	public default byte[] toBytes() {
		List<byte[]> l = new ArrayList<>();
		ABytesNode n = (ABytesNode) this;
		ABytesNode last = null;
		while(n!=null) {
			if(last == null || last == n.getMiddle())
				l.add(n.key());
			last = n;
			n = n.getParent();
		}
		Collections.reverse(l);
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		for(byte[] b : l)
			buffer.put(b);
		buffer.flip();

		byte[] out = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.limit());

		buffer.free();
		return out;
	}

	public default String toValue() {
		List<byte[]> l = new ArrayList<>();
		ABytesNode n = (ABytesNode) this;
		ABytesNode last = null;
		while(n!=null) {
			if(last == null || last == n.getMiddle()) {
				l.add(n.key());
			}
			last = n;
			n = n.getParent();
		}
		Collections.reverse(l);
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		for(byte[] b : l) {
			buffer.put(b);
		}
		buffer.flip();
		String str = new String(buffer.array(), buffer.arrayOffset(), buffer.limit());
		buffer.free();
		return str;
	}
}
