package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.mina.ChunkingOutputStream;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.primitives.Ints;
import jetbrains.exodus.env.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.IteratorUtils;

/**
 * Store for big files. Files are stored in chunks of 100MB, it therefore requires two stores: one for metadata maintained in {@link BigStoreMetaKeys} the other for the data. BigStoreMeta contains a list of {@link UUID} which describe a single value in the store, to be read in order.
 */
@Getter
public class BigStore<KEY, VALUE> implements Store<KEY, VALUE>, Closeable {

	public static final String META = "_META";
	public static final String DATA = "_DATA";
	private final SerializingStore<KEY, BigStoreMetaKeys> metaStore;
	private final SerializingStore<UUID, byte[]> dataStore;
	private final ObjectWriter valueWriter;
	private final XodusStore metaXodusStore;
	private final XodusStore dataXodusStore;
	private ObjectReader valueReader;

	private final StoreInfo<KEY, VALUE> storeInfo;

	@Getter @Setter
	private int chunkByteSize;


	public BigStore(XodusStoreFactory config,
					Validator validator,
					Environment env,
					StoreInfo<KEY, VALUE> storeInfo,
					Consumer<XodusStore> storeCloseHook,
					Consumer<XodusStore> storeRemoveHook,
					ObjectMapper mapper) {
		this.storeInfo = storeInfo;

		// Recommendation by the author of Xodus is to have logFileSize at least be 4 times the biggest file size.
		this.chunkByteSize = Ints.checkedCast(config.getXodus().getLogFileSize().toBytes() / 4L);

		metaXodusStore = new XodusStore(env, storeInfo.getName() + "_META", storeCloseHook, storeRemoveHook);
		metaStore = new SerializingStore<>(
				metaXodusStore,
				validator,
				mapper,
				storeInfo.getKeyType(),
				BigStoreMetaKeys.class,
				config.isValidateOnWrite(),
				config.isRemoveUnreadableFromStore(),
				config.getUnreadableDataDumpDirectory()

		);

		dataXodusStore = new XodusStore(env, storeInfo.getName() + "_DATA", storeCloseHook, storeRemoveHook);
		dataStore = new SerializingStore<>(
				dataXodusStore,
				validator,
				mapper,
				UUID.class,
				byte[].class,
				config.isValidateOnWrite(),
				config.isRemoveUnreadableFromStore(),
				config.getUnreadableDataDumpDirectory()
		);



		this.valueWriter = mapper.writerFor(storeInfo.getValueType());
		this.valueReader = mapper.readerFor(storeInfo.getValueType());
	}

	@Override
	public void add(KEY key, VALUE value) {
		if (metaStore.get(key) != null) {
			throw new IllegalArgumentException("There is already a value associated with " + key);
		}

		metaStore.update(key, writeValue(value));
	}

	@Override
	public VALUE get(KEY key) {
		BigStoreMetaKeys meta = metaStore.get(key);
		if (meta == null) {
			return null;
		}
		return createValue(key, meta);
	}

	@Override
	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		return metaStore.forEach((key, value, length) -> consumer.accept(key, createValue(key, value), length));
	}

	@Override
	public void update(KEY key, VALUE value) {
		remove(key);
		add(key, value);
	}

	@Override
	public void remove(KEY key) {
		BigStoreMetaKeys meta = metaStore.get(key);

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

	private BigStoreMetaKeys writeValue(VALUE value) {
		try {
			AtomicLong size = new AtomicLong();
			List<UUID> uuids = new ArrayList<>();

			ChunkingOutputStream cos = new ChunkingOutputStream(
					chunkByteSize,
					chunk -> {
						try {
							// Write chunks and accumulate their size.
							UUID id = UUID.randomUUID();
							uuids.add(id);
							dataStore.add(id, chunk);
							size.addAndGet(chunk.length);
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
			return new BigStoreMetaKeys(uuids.toArray(new UUID[0]), size.get());
		} catch (Exception e) {
			throw new RuntimeException("Failed to write " + value, e);
		}
	}

	private VALUE createValue(KEY key, BigStoreMetaKeys meta) {
		Iterator<ByteArrayInputStream> it = meta.loadData(dataStore)
												.map(ByteArrayInputStream::new)
												.iterator();

		try (InputStream in = new BufferedInputStream(new SequenceInputStream(IteratorUtils.asEnumeration(it)))) {
			return valueReader.readValue(in);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read " + key, e);
		}
	}

	@Override
	public void close() throws IOException {
		metaStore.close();
		dataStore.close();
	}

	@Getter
	@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
	public static class BigStoreMetaKeys {
		@NotEmpty
		private final UUID[] parts;
		private final long size;

		public Stream<byte[]> loadData(SerializingStore<UUID, byte[]> dataStore) {
			return Arrays.stream(parts).map(dataStore::get);
		}
	}

	@Override
	public String toString() {
		return "big " + storeInfo.getName() + "(" + storeInfo.getValueType().getSimpleName() + ")";
	}


	@Override
	public void clear() {
		metaStore.clear();
		dataStore.clear();
	}

	@Override
	public void deleteStore() {
		metaStore.deleteStore();
		dataStore.deleteStore();
	}
}
