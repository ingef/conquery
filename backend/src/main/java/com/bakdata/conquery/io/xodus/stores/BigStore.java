package com.bakdata.conquery.io.xodus.stores;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.mina.ChunkingOutputStream;
import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.primitives.Ints;
import io.dropwizard.util.Size;
import jetbrains.exodus.env.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Store for big files. Files are stored in chunks of 100MB, it therefore requires two stores: one for metadata maintained in {@link BigStoreMetaKey} the other for the data. BigStoreMeta contains a list of {@link UUID} which describe a single value in the store, to be read in order.
 */
@Slf4j
@Getter
public class BigStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final SerializingStore<KEY, BigStoreMetaKey> metaStore;
	private final SerializingStore<UUID, byte[]> dataStore;
	private final ObjectWriter valueWriter;
	private ObjectReader valueReader;

	private final StoreInfo storeInfo;

	@Getter
	@Setter
	private int chunkSize = Ints.checkedCast(Size.megabytes(100).toBytes());


	public BigStore(Validator validator, Environment env, StoreInfo storeInfo) {
		this.storeInfo = storeInfo;

		final SimpleStoreInfo metaStoreInfo = new SimpleStoreInfo(
				storeInfo.getXodusName() + "_META",
				storeInfo.getKeyType(),
				BigStoreMetaKey.class
		);

		metaStore = new SerializingStore<>(
				new XodusStore(env, metaStoreInfo), validator,
				metaStoreInfo
		);

		final SimpleStoreInfo dataStoreInfo = new SimpleStoreInfo(
				storeInfo.getXodusName() + "_DATA",
				UUID.class,
				byte[].class
		);

		dataStore = new SerializingStore<>(
				new XodusStore(env, dataStoreInfo), validator,
				dataStoreInfo
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
		if (metaStore.get(key) != null) {
			throw new IllegalArgumentException("There is already a value associated with " + key);
		}

		metaStore.update(key, new BigStoreMetaKey(writeValue(value)));
	}

	@Override
	public VALUE get(KEY key) {
		BigStoreMetaKey meta = metaStore.get(key);
		if (meta == null) {
			return null;
		}
		return createValue(key, meta);
	}

	@Override
	public void forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		metaStore.forEach((key, value, length) -> {
			consumer.accept(key, createValue(key, value), length);
		});
	}

	@Override
	public void update(KEY key, VALUE value) throws JSONException {
		remove(key);
		add(key, value);
	}

	@Override
	public void remove(KEY key) {
		BigStoreMetaKey meta = metaStore.get(key);

		if (meta == null) {
			return;
		}

		for (UUID id : meta.getParts()) {
			dataStore.remove(id);
		}
		metaStore.remove(key);
	}

	@Override
	public void fillCache() {
	}

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
		List<KEY> out = new ArrayList<>();
		metaStore.forEach((key, value, size) -> out.add(key));
		return out;
	}

	private UUID[] writeValue(VALUE value) {
		try {
			List<UUID> uuids = new ArrayList<>();
			ChunkingOutputStream cos = new ChunkingOutputStream(
					chunkSize,
					chunk -> {
						try {
							UUID id = UUID.randomUUID();
							uuids.add(id);
							dataStore.add(id, chunk);
						}
						catch (Exception e) {
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
			return uuids.toArray(new UUID[0]);
		} catch (Exception e) {
			throw new RuntimeException("Failed to write " + value, e);
		}
	}

	private VALUE createValue(KEY key, BigStoreMetaKey meta) {
		Iterator<ByteArrayInputStream> it = meta.loadData(dataStore)
												.map(ByteArrayInputStream::new)
												.iterator();

		try (InputStream in = new BufferedInputStream(new SequenceInputStream(IteratorUtils.asEnumeration(it)))) {
			return valueReader.readValue(in);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read " + key, e);
		}
	}

	@Getter
	@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
	public static class BigStoreMetaKey {
		@NotEmpty
		private final UUID[] parts;

		public Stream<byte[]> loadData(SerializingStore<UUID, byte[]> dataStore) {
			return Arrays.stream(parts).map(dataStore::get);
		}
	}

	@Override
	public void inject(Injectable injectable) {
		valueReader = injectable.injectInto(valueReader);
	}

	@Override
	public String toString() {
		return "big " + storeInfo.getXodusName() + "(" + storeInfo.getValueType().getSimpleName() + ")";
	}
}
