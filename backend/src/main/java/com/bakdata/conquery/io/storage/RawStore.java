package com.bakdata.conquery.io.storage;

import java.util.function.BiConsumer;

public interface RawStore {
	boolean add(byte[] writeKey, byte[] writeValue);

	byte[] get(byte[] writeKey);

	String getName();

	void forEach(BiConsumer<byte[], byte[]> consumer);

	boolean remove(byte[] bytes);

	boolean update(byte[] writeKey, byte[] writeValue);

	void clear();

	void deleteStore();

	void close();

	int count();
}
