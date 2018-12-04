package com.bakdata.conquery.util.io;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class MultiByteBuffer<K> implements Closeable {

	private final Map<K, GroupingByteBuffer> map = new HashMap<>();
	
	public MultiByteBuffer(Collection<K> keys, BiConsumer<K, byte[]> bufferConsumer) {
		for(K k:keys) {
			map.put(k, new GroupingByteBuffer(b->bufferConsumer.accept(k, b)));
		}
	}
	
	public GroupingByteBuffer get(K key) {
		return map.get(key);
	}
	
	@Override
	public void close() {
		map.values().forEach(GroupingByteBuffer::close);
	}

}
