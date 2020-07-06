package com.bakdata.conquery.io.xodus.stores;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Throwables;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
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

	private int totalProcessed = 0;
	private int failedKeys = 0;
	private int failedValues = 0;

	/**
	 * If set, all values that cannot be read are dumped as single files into this directory.
	 */
	private final File unreadableValuesDumpDir;
	
	private final boolean removeUnreadablesFromUnderlyingStore;

	@SuppressWarnings("unchecked")
	public SerializingStore(XodusStore store, Validator validator, IStoreInfo storeInfo) {
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
		
		removeUnreadablesFromUnderlyingStore = ConqueryConfig.getInstance().getStorage().isRemoveUnreadablesFromStore();
		
		// Prepare dump directory if there is one set in the config
		Optional<File> dumpUnreadable = ConqueryConfig.getInstance().getStorage().getUnreadbleDataDumpDirectory();
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
	public void close() throws IOException {
		store.close();
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

	@Override
	public void forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		totalProcessed = 0;
		failedKeys = 0;
		failedValues = 0;
		ArrayList<ByteIterable> unreadables = new ArrayList<>();
		store.forEach((k, v) -> {
			totalProcessed++;
			try {
				try {
					consumer.accept(readKey(k), readValue(v), v.getLength());
				} catch (Exception e) {
					if(unreadableValuesDumpDir != null) {						
						dumpToFile(v, Jackson.BINARY_MAPPER.readerFor(String.class).readValue(k.getBytesUnsafe()), unreadableValuesDumpDir, storeInfo.getXodusName());
					} else {
						log.warn("Could not parse value for key " + readKey(k), e);						
					}
					if(removeUnreadablesFromUnderlyingStore) {
						unreadables.add(k);
					}
					failedValues++;
				}
			} catch (Exception e) {
				log.warn("Could not parse key " + k, e);
				failedKeys++;
			}
		});
		// Print some statistics
		log.info(String.format("While processing store %s:\n\tEntries processed:\t%d\n\tKey read failure:\t%d (%.2f%%)\n\tValue read failure:\t%d (%.2f%%)",
			this.storeInfo.getXodusName(),
			totalProcessed, failedKeys,
			(float) failedKeys/totalProcessed*100,
			failedValues,
			(float) failedValues/totalProcessed*100));
		
		if(removeUnreadablesFromUnderlyingStore) {
			log.info("Removing the following unreadable elements from the store {}: {}", storeInfo.getXodusName(), unreadables.stream()
				.map(ByteIterable::getBytesUnsafe)
				.map(String::new)
				.collect(Collectors.toList()));
			unreadables.forEach(store::remove);			
		}
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
		log.trace("Removing value to key {} from store", key, storeInfo.getXodusName());
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
		File dumpfile = new File(makeDumpfileName(keyOfDump, unreadableDumpDir, storeName));
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

	private static String makeDumpfileName(String keyOfDump, File unreadableDumpDir, String storeName) {
		return Path.of(unreadableDumpDir.getAbsolutePath(), String.format("%s-%s-%s.json",
			DateTimeFormatter.BASIC_ISO_DATE.format(LocalDateTime.now()),
			storeName,
			keyOfDump
			)).toString().replaceAll("[\\\\/:*?\"<>|]", "");
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
}
