package com.bakdata.conquery.util.dict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;

import com.esotericsoftware.kryo.util.IntMap.Entry;

public interface ValueNode {

	public ABytesNode getParent();
	public int getValue();
	
	public default Entry<String> toEntry() {
		Entry<String> e = new Entry<>();
		e.key = this.getValue();
		e.value = toValue();
		return e;
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
