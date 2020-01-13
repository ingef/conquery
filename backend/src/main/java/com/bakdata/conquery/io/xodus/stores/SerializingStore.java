package com.bakdata.conquery.io.xodus.stores;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
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
		return readValue(store.get(writeKey(key)));
	}

	@Override
	public void forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		store.forEach((k, v) -> {
			try {
				try {
					consumer.accept(readKey(k), readValue(v), v.getLength());
				} catch (Exception e) {
					log.warn("Could not parse value for key " + readKey(k), e);
				}
			} catch (Exception e) {
				log.warn("Could not parse key " + k, e);
			}
		});
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
