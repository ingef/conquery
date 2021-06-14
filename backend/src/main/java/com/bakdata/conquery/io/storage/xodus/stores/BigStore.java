package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.mina.ChunkingOutputStream;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreInfo;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.primitives.Ints;
import jetbrains.exodus.env.Environment;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Store for big files. Files are stored in chunks of 100MB, it therefore requires two stores: one for metadata maintained in the other for the data. BigStoreMeta contains a list of {@link UUID} which describe a single value in the store, to be read in order.
 */
@Getter
@Slf4j
public class BigStore<KEY, VALUE> implements Store<KEY, VALUE>, Closeable {

	private final SerializingStore<KEY, KeysContainer> metaStore;
	private final SerializingStore<UUID, byte[]> dataStore;
	private final ObjectWriter valueWriter;
	private ObjectReader valueReader;

	private final StoreInfo storeInfo;

	@Getter
	@Setter
	private int chunkByteSize;

	private final ExecutorService service = Executors.newCachedThreadPool();


	public BigStore(XodusStoreFactory config, Validator validator, Environment env, StoreInfo storeInfo, Collection<jetbrains.exodus.env.Store> openStores, Consumer<Environment> envCloseHook, Consumer<Environment> envRemoveHook, ObjectMapper mapper) {
		this.storeInfo = storeInfo;

		// Recommendation by the author of Xodus is to have logFileSize at least be 4 times the biggest file size.
		this.chunkByteSize = Ints.checkedCast(config.getXodus().getLogFileSize().toBytes() / 4L);

		final SimpleStoreInfo metaStoreInfo = new SimpleStoreInfo(
				storeInfo.getName() + "_META",
				storeInfo.getKeyType(),
				KeysContainer.class
		);

		metaStore = new SerializingStore<>(
				config,
				new XodusStore(env, metaStoreInfo, openStores, envCloseHook, envRemoveHook), validator,
				metaStoreInfo,
				mapper
		);

		final SimpleStoreInfo dataStoreInfo = new SimpleStoreInfo(
				storeInfo.getName() + "_DATA",
				UUID.class,
				byte[].class
		);

		dataStore = new SerializingStore<>(
				config,
				new XodusStore(env, dataStoreInfo, openStores, envCloseHook, envRemoveHook), validator,
				dataStoreInfo,
				mapper
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
		KeysContainer meta = metaStore.get(key);
		if (meta == null) {
			return null;
		}
		return createValue(key, meta);
	}

	@Override
	@SneakyThrows
	public IterationStatistic forEach(BiConsumer<KEY, VALUE> consumer) {


		final IterationStatistic statistic = metaStore.forEach((key, chunkKeys) -> {
			service.submit(() -> {
				final VALUE value1 = createValue(key, chunkKeys);
				consumer.accept(key, value1);
			});
		});

		service.shutdown();

		while (!service.awaitTermination(30, TimeUnit.SECONDS)) {
			log.debug("Still waiting for {} to load.", this);
		}

		return statistic;
	}

	@Override
	public void update(KEY key, VALUE value) {
		remove(key);
		add(key, value);
	}

	@Override
	public void remove(KEY key) {
		KeysContainer parts = metaStore.get(key);

		if (parts == null) {
			return;
		}

		for (UUID id : parts.getParts()) {
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
		metaStore.forEach((key, value) -> out.add(key));
		return out;
	}

	private KeysContainer writeValue(VALUE value) {
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
						os,
						value
				);
			}
			return new KeysContainer(uuids.toArray(new UUID[0]), -1);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to write " + value, e);
		}
	}

	private VALUE createValue(KEY key, KeysContainer meta) {

		try {
			return loadData(meta.parts).get();
		}
		catch (InterruptedException | ExecutionException e) {
			log.error("Failed to load {}", key, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		metaStore.close();
		dataStore.close();
	}

	@SneakyThrows
	private CompletableFuture<VALUE> loadData(UUID[] parts) {
		final PipedInputStream sink = new PipedInputStream();
		final PipedOutputStream outputStream = new PipedOutputStream(sink);

		// First is just a dummy.
		CompletableFuture<Void> current = CompletableFuture.completedFuture(null);

		for (int i = 0; i < parts.length; i++) {
			final UUID id = parts[i];
			final int curIdx = i;

			// Submit chunk loading
			CompletableFuture<byte[]> part = CompletableFuture.supplyAsync(() -> {
				final byte[] out = dataStore.get(id);
				log.trace("idx = {} loaded", curIdx);
				return out;
			}, service);

			// Then weave jobs to pipe in-order, by concatenating by the dummy.
			current = current.thenAcceptBoth(part, (ignored, incoming) -> {
				try {
					log.trace("idx = {} written", curIdx);
					outputStream.write(incoming);
				}
				catch (IOException e) {
					log.error("Failed to write chunk", e);
				}
			});
		}

		final CompletableFuture<VALUE> out = CompletableFuture.supplyAsync(() -> {
			try {
				return valueReader.readValue(sink);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, service);

		current.whenComplete((ignored, exc) -> {
			if (exc != null) {
				log.error("Error loading {}", parts, exc);
				out.completeExceptionally(exc);
				return;
			}

			try {
				log.trace("{} all done", parts);
				outputStream.flush();
				outputStream.close();
			}
			catch (IOException e) {
				log.error("Failed to flush", e);
			}
		});

		return out;
	}

	@Override
	public void inject(Injectable injectable) {
		valueReader = injectable.injectInto(valueReader);
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
	public void removeStore() {
		metaStore.removeStore();
		dataStore.removeStore();
	}

	@Data
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	static class KeysContainer {
		private final UUID[] parts;
		private final int size; //TODO unused
	}
}
