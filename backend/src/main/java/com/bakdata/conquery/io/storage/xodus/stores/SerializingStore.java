package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Throwables;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Key-value-store from {@link KEY} type values to {@link VALUE} values. ACID consistent, stored on disk using {@link jetbrains.exodus.env.Store} via {@link XodusStore}.
 * <p>
 * Values are (de-)serialized using {@linkplain ObjectMapper}.
 *
 * @param <KEY>   type of keys
 * @param <VALUE> type of values.
 */
@Slf4j
@ToString(of = "store")
public class SerializingStore<KEY, VALUE> implements Store<KEY, VALUE> {

	public static final String DUMP_FILE_EXTENSION = "json";
	public static final String EXCEPTION_FILE_EXTENSION = "exception";

	/**
	 * Used for serializing keys.
	 */
	private final ObjectWriter keyWriter;

	/**
	 * Deserializer for keys
	 */
	private final ThreadLocal<ObjectReader> keyReader;

	/**
	 * Serializer for values
	 */
	private final ObjectWriter valueWriter;

	/**
	 * Deserializer for values
	 */
	private final ThreadLocal<ObjectReader> valueReader;

	/**
	 * Optional validator used for serialization.
	 */
	private final Validator validator;

	/**
	 * The underlying store to write the values to.
	 */
	private final XodusStore store;

	/**
	 *
	 */
	private final Class<VALUE> valueType;

	/**
	 * Validate elements on write
	 */
	private final boolean validateOnWrite;


	/**
	 * If set, all values that cannot be read are dumped as single files into this directory.
	 */
	private final File unreadableValuesDumpDir;

	private final boolean removeUnreadablesFromUnderlyingStore;

	private final ObjectMapper objectMapper;

	public <CLASS_K extends Class<KEY>, CLASS_V extends Class<VALUE>> SerializingStore(XodusStore store,
																					   Validator validator,
																					   ObjectMapper objectMapper,
																					   CLASS_K keyType,
																					   CLASS_V valueType,
																					   boolean validateOnWrite,
																					   boolean removeUnreadableFromStore,
																					   File unreadableDataDumpDirectory) {
		this.store = store;
		this.validator = validator;
		this.validateOnWrite = validateOnWrite;

		this.valueType = valueType;

		this.objectMapper = objectMapper;

		valueWriter = objectMapper.writerFor(this.valueType);

		valueReader = ThreadLocal.withInitial(() -> objectMapper.readerFor(this.valueType));

		keyWriter = objectMapper.writerFor(keyType);

		keyReader = ThreadLocal.withInitial(() -> objectMapper.readerFor(keyType));

		removeUnreadablesFromUnderlyingStore = removeUnreadableFromStore;

		unreadableValuesDumpDir = unreadableDataDumpDirectory;

		if (shouldDumpUnreadables()) {
			if (!unreadableValuesDumpDir.exists() && !unreadableValuesDumpDir.mkdirs()) {
				throw new IllegalStateException("Could not create dump directory: " + unreadableValuesDumpDir);
			}
			else if (!unreadableValuesDumpDir.isDirectory()) {
				throw new IllegalArgumentException(String.format("The provided path points to an existing file which is not a directory. Was: %s", unreadableValuesDumpDir.getAbsolutePath()));
			}
		}
	}

	private boolean shouldDumpUnreadables() {
		return unreadableValuesDumpDir != null;
	}

	@Override
	public void add(KEY key, VALUE value) {
		if (!valueType.isInstance(value)) {
			throw new IllegalStateException("The element " + value + " is not of the required type " + valueType);
		}
		if (validateOnWrite) {
			ValidatorHelper.failOnError(log, validator.validate(value));
		}

		store.add(writeKey(key), writeValue(value));
	}

	/**
	 * Serialize key with {@code keyWriter}.
	 */
	private ByteIterable writeKey(KEY key) {
		return write(key, keyWriter);
	}

	/**
	 * Serialize value with {@code valueWriter}.
	 */
	private ByteIterable writeValue(VALUE value) {
		return write(value, valueWriter);
	}

	/**
	 * Try writing object with writer.
	 */
	private ByteIterable write(Object obj, ObjectWriter writer) {
		try {
			final byte[] bytes = writer.writeValueAsBytes(obj);
			if (log.isTraceEnabled()) {
				final String json = JacksonUtil.toJsonDebug(bytes);
				log.trace("Written ({}): {}", valueType.getName(), json);
			}
			return new ArrayByteIterable(bytes);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to write " + obj, e);
		}
	}

	@Override
	public VALUE get(KEY key) {
		final ByteIterable binValue = store.get(writeKey(key));

		try {
			return readValue(binValue);
		}
		catch (Exception e) {

			if (unreadableValuesDumpDir != null) {
				dumpToFile(binValue, key.toString(), e, unreadableValuesDumpDir, store.getName(), objectMapper);
			}

			if (removeUnreadablesFromUnderlyingStore) {
				remove(key);
				// Null seems to be an acceptable return value in this case
				return null;
			}

			Throwables.throwIfUnchecked(e);

			throw new RuntimeException(e);
		}
	}

	/**
	 * Deserialize value with {@code valueReader}.
	 */
	private VALUE readValue(ByteIterable value) {
		return read(valueReader.get(), value);
	}

	/**
	 * Dumps the content of an unreadable value to a file as a json (it tries to parse it as an object and than tries to dump it as a json).
	 *
	 * @param obj               The object to dump.
	 * @param keyOfDump         The key under which the unreadable value is accessible. It is used for the file name.
	 * @param reason            The exception causing us to dump the file
	 * @param unreadableDumpDir The director to dump to. The method assumes that the directory exists and is okay to write to.
	 * @param storeName         The name of the store which is also used in the dump file name.
	 */
	private static void dumpToFile(@NonNull ByteIterable obj, @NonNull String keyOfDump, Exception reason, @NonNull File unreadableDumpDir, String storeName, ObjectMapper objectMapper) {
		// Create dump filehandle
		final File dumpfile = makeDumpFileName(keyOfDump, unreadableDumpDir, storeName);
		final File exceptionFileName = makeExceptionFileName(keyOfDump, unreadableDumpDir, storeName);

		if (dumpfile.exists() || exceptionFileName.exists()) {
			log.trace("Abort dumping of file {} because it already exists.", dumpfile);
			return;
		}

		if (!dumpfile.getParentFile().exists() && !dumpfile.getParentFile().mkdirs()) {
			throw new IllegalStateException("Could not create `%s`.".formatted(dumpfile.getParentFile()));
		}

		//TODO FK: dump in a separate thread so we are not blocking the reader thread.

		// Write json
		try {
			log.info("Dumping value of key {} to {} (because it cannot be deserialized anymore).", keyOfDump, dumpfile.getCanonicalPath());

			final JsonNode dump = objectMapper.readerFor(JsonNode.class).readValue(obj.getBytesUnsafe(), 0, obj.getLength());
			Jackson.MAPPER.writer().writeValue(dumpfile, dump);
		}
		catch (IOException e) {
			log.error("Failed to dump unreadable value of key `{}` to file `{}`", keyOfDump, dumpfile, e);
		}

		try (PrintStream out = new PrintStream(exceptionFileName)) {
			reason.printStackTrace(out);
		}
		catch (IOException e) {
			log.error("Failed to dump exception for `{}` to file `{}`.", keyOfDump, exceptionFileName, e);
		}

	}

	@Override
	public void remove(KEY key) {
		log.trace("Removing value to key {} from Store[{}]", key, store.getName());
		store.remove(writeKey(key));
	}

	/**
	 * Try read value with reader.
	 */
	private <T> T read(ObjectReader reader, ByteIterable obj) {
		if (obj == null) {
			return null;
		}
		try {
			return reader.readValue(obj.getBytesUnsafe(), 0, obj.getLength());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read " + JacksonUtil.toJsonDebug(obj.getBytesUnsafe()), e);
		}
	}

	/**
	 * Generates a valid file name from the key of the dump object, the store and the current time.
	 * However, it does not ensure that there is no file with such a name.
	 * <p>
	 * Current implementation is `$unreadableDumpDir/$today/$store/$key.json`
	 */
	@NotNull
	public static File makeDumpFileName(@NotNull String keyOfDump, @NotNull File unreadableDumpDir, @NotNull String storeName) {
		return unreadableDumpDir.toPath()
								.resolve(DateTimeFormatter.BASIC_ISO_DATE.format(LocalDateTime.now()))
								.resolve(storeName)
								.resolve(sanitiseFileName(keyOfDump) + "." + DUMP_FILE_EXTENSION)
								.toFile();

	}

	/**
	 * Generates a valid file name from the key of the dump object, the store and the current time.
	 * However, it does not ensure that there is no file with such a name.
	 * <p>
	 * Current implementation is `$unreadableDumpDir/$today/$store/$key.exception`
	 */
	@NotNull
	public static File makeExceptionFileName(@NotNull String keyOfDump, @NotNull File unreadableDumpDir, @NotNull String storeName) {
		return unreadableDumpDir.toPath()
								.resolve(DateTimeFormatter.BASIC_ISO_DATE.format(LocalDateTime.now()))
								.resolve(storeName)
								.resolve(sanitiseFileName(keyOfDump) + "." + EXCEPTION_FILE_EXTENSION)
								.toFile();

	}

	private static String sanitiseFileName(@NotNull String name) {
		return FileUtil.SAVE_FILENAME_REPLACEMENT_MATCHER.matcher(name).replaceAll("_");
	}

	/**
	 * Iterates a given consumer over the entries of this store.
	 * Depending on the {@link XodusStoreFactory} corrupt entries may be dump to a file and/or removed from the store.
	 * These entries are not submitted to the consumer.
	 */
	@SneakyThrows
	@Override
	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		final IterationStatistic result = new IterationStatistic();
		final ArrayList<ByteIterable> unreadables = new ArrayList<>();

		final ExecutorService executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
																	   60L, TimeUnit.SECONDS,
																	   new ArrayBlockingQueue<>(100)
		);

		store.forEach((k, v) -> {
			executorService.submit(() -> {

				result.incrTotalProcessed();

				// Try to read the key first
				final KEY key = getDeserializedAndDumpFailed(
						k,
						this::readKey,
						() -> new String(k.getBytesUnsafe()),
						v,
						"Could not parse key [{}]"
				);
				if (key == null) {
					unreadables.add(k);
					result.incrFailedKeys();
					return;
				}

				// Try to read the value
				final VALUE value = getDeserializedAndDumpFailed(
						v,
						this::readValue,
						key::toString,
						v,
						"Could not parse value for key [{}]"
				);

				if (value == null) {
					unreadables.add(k);
					result.incrFailedValues();
					return;
				}

				// Apply the consumer to key and value
				try {
					consumer.accept(key, value, v.getLength());
				}
				catch (Exception e) {
					log.warn("Unable to apply for-each consumer on key[{}]", key, e);
				}
			});
		});

		executorService.shutdown();

		while (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Still waiting for {} to load.", this);
		}

		// Print some statistics
		final int total = result.getTotalProcessed();
		log.debug(
				String.format(
						"While processing store %s:\n\tEntries processed:\t%d\n\tKey read failure:\t%d (%.2f%%)\n\tValue read failure:\t%d (%.2f%%)",
						store.getName(),
						total,
						result.getFailedKeys(),
						total > 0 ? (float) result.getFailedKeys() / total * 100 : 0,
						result.getFailedValues(),
						total > 0 ? (float) result.getFailedValues() / total * 100 : 0
				));

		// Remove corrupted entries from the store if configured so
		if (removeUnreadablesFromUnderlyingStore) {
			log.warn("Removing {} unreadable elements from the store {}.", unreadables.size(), store.getName());
			unreadables.forEach(store::remove);
		}
		return result;
	}

	/**
	 * Deserializes the gives serial value (either a key or a value of an store entry) to a concrete object. If that fails the entry-value is dumped if configured so to a file using the entry-key for the filename.
	 *
	 * @param <TYPE>                  The deserialized object type.
	 * @param serial                  The to be deserialized object (key or value of an entry)
	 * @param deserializer            The concrete deserializer to use.
	 * @param onFailKeyStringSupplier When deserilization failed and dump is enabled this is used in the dump file name.
	 * @param onFailOrigValue         Will be the dumpfile content rendered as a json.
	 * @param onFailWarnMsgFmt        The warning message that will be logged on failure.
	 * @return The deserialized value
	 */
	private <TYPE> TYPE getDeserializedAndDumpFailed(ByteIterable serial, Function<ByteIterable, TYPE> deserializer, Supplier<String> onFailKeyStringSupplier, ByteIterable onFailOrigValue, String onFailWarnMsgFmt) {
		try {
			return deserializer.apply(serial);
		}
		catch (Exception e) {
			// With trace also print the stacktrace
			log.warn(onFailWarnMsgFmt, onFailKeyStringSupplier.get(), log.isTraceEnabled() ? e : null);

			if (shouldDumpUnreadables()) {
				dumpToFile(onFailOrigValue, onFailKeyStringSupplier.get(), e, unreadableValuesDumpDir, store.getName(), objectMapper);
			}
		}
		return null;
	}

	/**
	 * Deserialize value with {@code keyReader}.
	 */
	private KEY readKey(ByteIterable key) {
		return read(keyReader.get(), key);
	}

	@Override
	public void update(KEY key, VALUE value) {
		if (!valueType.isInstance(value)) {
			throw new IllegalStateException("The element " + value + " is not of the required type " + valueType);
		}

		if (validateOnWrite) {
			ValidatorHelper.failOnError(log, validator.validate(value));
		}

		store.update(writeKey(key), writeValue(value));
	}

	@Override
	public void fillCache() {
	}

	@Override
	public int count() {
		return store.count();
	}

	@Override
	public Collection<VALUE> getAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<KEY> getAllKeys() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		store.clear();
	}

	@Override
	public void deleteStore() {
		store.deleteStore();
	}

	@Override
	public void close() {
		store.close();
	}

	@Data
	public static class IterationStatistic {
		private int totalProcessed;
		private int failedKeys;
		private int failedValues;

		public void incrTotalProcessed() {
			totalProcessed++;
		}

		public void incrFailedKeys() {
			failedKeys++;
		}

		public void incrFailedValues() {
			failedValues++;
		}
	}
}
