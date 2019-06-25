package com.bakdata.conquery.io.xodus.stores;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.validation.Validator;

import org.apache.commons.collections4.IteratorUtils;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.mina.ChunkingOutputStream;
import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.primitives.Ints;

import io.dropwizard.util.Size;
import jetbrains.exodus.env.Environment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Getter
public class BigStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final MPStore<KEY, BigStoreMeta> metaStore;
	private final MPStore<UUID, byte[]> dataStore;
	private final ObjectWriter valueWriter;
	private ObjectReader valueReader;
	@Getter @Setter
	private int chunkSize = Ints.checkedCast(Size.megabytes(100).toBytes());
	private final StoreInfo storeInfo;
	
	

	public BigStore(Validator validator, Environment env, StoreInfo storeInfo) {
		this.storeInfo = storeInfo;
		metaStore = new MPStore<>(
				validator,
				env,
				new SimpleStoreInfo(
					storeInfo.getXodusName()+"_META",
					storeInfo.getKeyType(),
					BigStoreMeta.class
				)
			);
		dataStore = new MPStore<>(
				validator,
				env,
				new SimpleStoreInfo(
					storeInfo.getXodusName()+"_DATA",
					UUID.class,
					byte[].class
				)
			);
		this.valueWriter = Jackson.BINARY_MAPPER.writerFor(storeInfo.getValueType());
		this.valueReader = Jackson.BINARY_MAPPER.readerFor(storeInfo.getValueType());
	}
	
	@Override
	public void close() throws IOException {
		metaStore.close();
		dataStore.close();
	}

	@Override
	public void add(KEY key, VALUE value) throws JSONException {
		BigStoreMeta meta = new BigStoreMeta();
		if(metaStore.get(key) != null) {
			throw new IllegalArgumentException("There is already a value associated with "+key);
		}
		
		meta.getParts().addAll(writeValue(value));
		metaStore.update(key, meta);
	}

	@Override
	public VALUE get(KEY key) {
		BigStoreMeta meta = metaStore.get(key);
		if(meta == null) {
			return null;
		}
		return createValue(key, meta);
	}

	@Override
	public void forEach(Consumer<StoreEntry<KEY, VALUE>> consumer) {
		metaStore.forEach(e -> {
			
			StoreEntry<KEY, VALUE> entry = new StoreEntry<>();
			entry.setKey(e.getKey());
			entry.setValue(createValue(e.getKey(), e.getValue()));
			entry.setByteSize(e.getValue().getSize().get());
			consumer.accept(entry);
		});
	}

	@Override
	public void update(KEY key, VALUE value) throws JSONException {
		remove(key);
		add(key, value);
	}

	@Override
	public void remove(KEY key) {
		BigStoreMeta meta = metaStore.get(key);
		if(meta != null) {
			for(UUID id:meta.getParts()) {
				dataStore.remove(id);
			}
			metaStore.remove(key);
		}
	}

	@Override
	public void fillCache() {}

	@Override
	public int count() {
		return metaStore.count();
	}

	@Override
	public Collection<VALUE> getAll() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Collection<KEY> getAllKeys() {
		List<KEY> l = new ArrayList<>();
		metaStore.forEach(e -> l.add(e.getKey()));
		return l;
	}
	
	private Collection<UUID> writeValue(VALUE value) {
		try {
			List<UUID> uuids = new ArrayList<>();
			ChunkingOutputStream cos = new ChunkingOutputStream(
				chunkSize,
				b -> {
					try {
						UUID id = UUID.randomUUID();
						uuids.add(id);
						dataStore.add(id, b);
					} catch(Exception e) {
						throw new RuntimeException("Failed to write chunk", e);
					}
				}
			);
			try (OutputStream os = cos) {
				valueWriter.writeValue(
						cos,
						value
				);
			}
			return uuids;
		} catch (Exception e) {
			throw new RuntimeException("Failed to write "+value, e);
		}
	}

	private VALUE createValue(KEY key, BigStoreMeta meta) {
		Iterator<ByteArrayInputStream> it = meta
				.loadData(dataStore)
				.map(ByteArrayInputStream::new)
				.iterator();
		try(InputStream in = new BufferedInputStream(new SequenceInputStream(IteratorUtils.asEnumeration(it)))) {
			return valueReader.readValue(in);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read "+key, e);
		}
	}

	@Getter
	public static class BigStoreMeta {
		@NotEmpty
		private final List<UUID> parts = new ArrayList<>();
		@JsonIgnore
		private transient AtomicLong size;
		
		public Stream<byte[]> loadData(MPStore<UUID, byte[]> dataStore) {
			size = new AtomicLong(0);
			return parts
				.stream()
				.map(dataStore::get)
				.peek(b -> size.addAndGet(b.length));
		}
	}
	
	@Override
	public void inject(Injectable injectable) {
		valueReader = injectable.injectInto(valueReader);
	}
	
	@Override
	public String toString() {
		return "big "+storeInfo.getXodusName()+"("+storeInfo.getValueType().getSimpleName()+")";
	}
}
