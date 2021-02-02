package com.bakdata.conquery.io.xodus.stores;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Throwables;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Key-value-store from {@link KEY} type values to {@link VALUE} values. ACID consistent, stored on disk using {@link jetbrains.exodus.env.Store} via {@link XodusStore}.
 * <p>
 * Values are (de-)serialized using {@linkplain Jackson.BINARY_MAPPER}.
 *
 * @param <KEY>   type of keys
 * @param <VALUE> type of values.
 */
@Slf4j
public class SerializingStore<KEY, VALUE> implements Store<KEY, VALUE> {

	public static final String DUMP_FILE_EXTENTION = "json";
	
	/**
	 * Used for serializing keys.
	 */
	private final ObjectWriter keyWriter;

	/**
	 * Deserializer for keys
	 */
	private final ObjectReader keyReader;

	/**
	 * Serializer for values
	 */
	private final ObjectWriter valueWriter;

	/**
	 * Deserializer for values
	 */
	private ObjectReader valueReader;

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
	 * Description of the store.
	 */
	private final IStoreInfo storeInfo;


	/**
	 * If set, all values that cannot be read are dumped as single files into this directory.
	 */
	private final File unreadableValuesDumpDir;
	
	private final boolean removeUnreadablesFromUnderlyingStore;

	@SuppressWarnings("unchecked")
	public SerializingStore(StorageConfig config, XodusStore store, Validator validator, IStoreInfo storeInfo) {
		this.storeInfo = storeInfo;
		this.store = store;
		this.validator = validator;

		valueType = (Class<VALUE>) storeInfo.getValueType();

		valueWriter = Jackson.BINARY_MAPPER
							  .writerFor(valueType)
							  .withView(InternalOnly.class);

		valueReader = Jackson.BINARY_MAPPER
							  .readerFor(valueType)
							  .withView(InternalOnly.class);

		keyWriter = Jackson.BINARY_MAPPER
							.writerFor(storeInfo.getKeyType())
							.withView(InternalOnly.class);

		keyReader = Jackson.BINARY_MAPPER
							.readerFor(storeInfo.getKeyType())
							.withView(InternalOnly.class);
		
		removeUnreadablesFromUnderlyingStore = config.isRemoveUnreadablesFromStore();
		
		// Prepare dump directory if there is one set in the config
		Optional<File> dumpUnreadable = config.getUnreadbleDataDumpDirectory();
		if(dumpUnreadable.isPresent()) {
			unreadableValuesDumpDir = dumpUnreadable.get();
			if(!unreadableValuesDumpDir.exists()) {
				unreadableValuesDumpDir.mkdirs();
			}
			else if(!unreadableValuesDumpDir.isDirectory()) {
				throw new IllegalArgumentException(String.format("The provided path points to an existing file which is not a directory. Was: %s", unreadableValuesDumpDir.getAbsolutePath()));
			}
		} else {
			unreadableValuesDumpDir = null;
		}
	}

	@Override
	public void add(KEY key, VALUE value) throws JSONException {
		if (!valueType.isInstance(value)) {
			throw new IllegalStateException("The element " + value + " is not of the required type " + valueType);
		}
		if (ConqueryConfig.getInstance().getStorage().isValidateOnWrite()) {
			ValidatorHelper.failOnError(log, validator.validate(value), "encoding " + value.getClass().getSimpleName() + " " + Objects.toString(value));
		}

		store.add(writeKey(key), writeValue(value));
	}

	@Override
	public VALUE get(KEY key) {
		ByteIterable binValue = store.get(writeKey(key));
		try {
			return readValue(binValue);			
		} catch (Exception e) {
			if(unreadableValuesDumpDir != null) {
				dumpToFile(binValue, key.toString(), unreadableValuesDumpDir, storeInfo.getXodusName());
			}
			if(removeUnreadablesFromUnderlyingStore) {
				remove(key);
				// Null seems to be an acceptable return value in this case
				return null;
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Iterates a given consumer over the entries of this store.
	 * Depending on the {@link StorageConfig} corrupt entries may be dump to a file and/or removed from the store.
	 * These entries are not submitted to the consumer.
	 */
	@Override
	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		IterationStatistic result = new IterationStatistic();
		ArrayList<ByteIterable> unreadables = new ArrayList<>();
		store.forEach((k, v) -> {
			result.incrTotalProcessed();

			// Try to read the key first
			KEY key = getDeserializedAndDumpFailed(
				k,
				this::readKey,
				() -> new String(k.getBytesUnsafe()),
				v,
				"Could not parse key [{}]");
			if (key == null) {
				unreadables.add(k);
				result.incrFailedKeys();
				return;
			}

			// Try to read the value
			VALUE value = getDeserializedAndDumpFailed(
				v, 
				this::readValue, 
				() -> key.toString(),
				v, 
				"Could not parse value for key [{}]");
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
		// Print some statistics
		int total = result.getTotalProcessed();
		log.debug(
			String.format(
				"While processing store %s:\n\tEntries processed:\t%d\n\tKey read failure:\t%d (%.2f%%)\n\tValue read failure:\t%d (%.2f%%)",
				this.storeInfo.getXodusName(),
				total,
				result.getFailedKeys(),
				total > 0 ? (float) result.getFailedKeys() / total * 100 : 0,
				result.getFailedValues(),
				total > 0 ? (float) result.getFailedValues() / total * 100 : 0));

		// Remove corrupted entries from the store if configured so
		if (removeUnreadablesFromUnderlyingStore) {
			log.warn("Removing {} unreadable elements from the store {}.", unreadables.size(), storeInfo.getXodusName());
			unreadables.forEach(store::remove);
		}
		return result;
	}
	
	/**
	 * Deserializes the gives serial value (either a key or a value of an store entry) to a concrete object. If that fails the entry-value is dumped if configured so to a file using the entry-key for the filename.
	 * @param <TYPE> The deserialized object type.
	 * @param serial The to be deserialized object (key or value of an entry)
	 * @param deserializer The concrete deserializer to use.
	 * @param onFailKeyStringSupplier When deserilization failed and dump is enabled this is used in the dump file name.
	 * @param onFailOrigValue Will be the dumpfile content rendered as a json.
	 * @param onFailWarnMsgFmt The warn message that will be logged on failure.
	 * @return
	 */
	private <TYPE> TYPE getDeserializedAndDumpFailed(ByteIterable serial, Function<ByteIterable, TYPE> deserializer, Supplier<String> onFailKeyStringSupplier, ByteIterable onFailOrigValue, String onFailWarnMsgFmt ){
		try {
			return deserializer.apply(serial);			
		} catch (Exception e) {
			if(unreadableValuesDumpDir != null) {
				dumpToFile(onFailOrigValue, onFailKeyStringSupplier.get(), unreadableValuesDumpDir, storeInfo.getXodusName());
			}
			if(log.isTraceEnabled()){
				// With trace also print the stacktrace
				log.trace(onFailWarnMsgFmt, onFailKeyStringSupplier.get(), e);
			} else {
				log.warn(onFailWarnMsgFmt, onFailKeyStringSupplier.get());
			}
		}
		return null;
	}

	@Override
	public void update(KEY key, VALUE value) throws JSONException {
		if (!valueType.isInstance(value)) {
			throw new IllegalStateException("The element " + value + " is not of the required type " + valueType);
		}

		if (ConqueryConfig.getInstance().getStorage().isValidateOnWrite()) {
			ValidatorHelper.failOnError(log, validator.validate(value), "encoding " + value.getClass().getSimpleName() + " " + Objects.toString(value));
		}

		store.update(writeKey(key), writeValue(value));
	}

	@Override
	public void remove(KEY key) {
		log.trace("Removing value to key {} from Store[{}]", key, storeInfo.getXodusName());
		store.remove(writeKey(key));
	}

	/**
	 * Serialize value with {@code valueWriter}.
	 */
	private ByteIterable writeValue(VALUE value) {
		return write(value, valueWriter);
	}

	/**
	 * Serialize key with {@code keyWriter}.
	 */
	private ByteIterable writeKey(KEY key) {
		return write(key, keyWriter);
	}

	/**
	 * Deserialize value with {@code valueReader}.
	 */
	private VALUE readValue(ByteIterable value) {
		return read(valueReader, value);
	}

	/**
	 * Deserialize value with {@code keyReader}.
	 */
	private KEY readKey(ByteIterable key) {
		return read(keyReader, key);
	}

	/**
	 * Try writing object with writer.
	 */
	private ByteIterable write(Object obj, ObjectWriter writer) {
		try {
			byte[] bytes = writer.writeValueAsBytes(obj);
			if (log.isTraceEnabled()) {
				String json = JacksonUtil.toJsonDebug(bytes);
				log.trace("Written Messagepack ({}): {}", valueType.getName(), json);
			}
			return new ArrayByteIterable(bytes);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to write " + obj, e);
		}
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
		} catch (IOException e) {
			throw new RuntimeException("Failed to read " + JacksonUtil.toJsonDebug(obj.getBytesUnsafe()), e);
		}
	}

	/**
	 * Dumps the content of an unreadable value to a file as a json (it tries to parse it as an object and than tries to dump it as a json).
	 * @param obj The object to dump.
	 * @param keyOfDump The key under which the unreadable value is accessible. It is used for the file name.
	 * @param unreadableDumpDir The director to dump to. The method assumes that the directory exists and is okay to write to.
	 * @param storeName The name of the store which is also used in the dump file name.
	 */
	private static void dumpToFile(@NonNull ByteIterable obj, @NonNull String keyOfDump, @NonNull File unreadableDumpDir, @NonNull String storeName) {
		// Create dump filehandle
		File dumpfile = new File(unreadableDumpDir, makeDumpfileName(keyOfDump, storeName));
		if(dumpfile.exists()) {
			log.warn("Abort dumping of file {} because it already exists.",dumpfile);
			return;
		}
		// Write dump
		try {
			log.info("Dumping value of key {} to {} (because it cannot be deserialized anymore).", keyOfDump, dumpfile.getCanonicalPath());
			JsonNode dump = Jackson.BINARY_MAPPER.readerFor(JsonNode.class).readValue(obj.getBytesUnsafe(), 0, obj.getLength());
			Jackson.MAPPER.writer().writeValue(dumpfile, dump);
		}
		catch (IOException e) {
			log.error("Unable to dump unreadable value of key `{}` to file `{}`",keyOfDump, dumpfile, e);
		}
	}

	/**
	 * Generates a valid file name from the key of the dump object, the store and the current time.
	 * However, it does not ensure that there is no file with such a name.
	 */
	private static String makeDumpfileName(String keyOfDump, String storeName) {
		return FileUtil.SAVE_FILENAME_REPLACEMENT_MATCHER.matcher(
			String.format("%s-%s-%s.%s",
				DateTimeFormatter.BASIC_ISO_DATE.format(LocalDateTime.now()),
				storeName,
				keyOfDump,
				DUMP_FILE_EXTENTION
			)).replaceAll("_");
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
	public void inject(Injectable injectable) {
		valueReader = injectable.injectInto(valueReader);
	}

	@Override
	public String toString() {
		return storeInfo.getXodusName() + "(" + storeInfo.getValueType().getSimpleName() + ")";
	}

	@Override
	public Collection<KEY> getAllKeys() {
		throw new UnsupportedOperationException();
	}
	
	@Data
	public static class IterationStatistic {
		private int totalProcessed = 0;
		private int failedKeys = 0;
		private int failedValues = 0;
		
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
