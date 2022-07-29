package com.bakdata.conquery.models.events.stores.specific.string;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Compacted String store, that uses two methods to reduce memory footprint:
 * 1. Use a byte efficient encoding string for the actual string. See {@link Encoding}
 * 2. Store the byte string in an appropriate data structure. See {{@link Dictionary and sub classes}}
 */
@Getter
@Setter
@Slf4j
@CPSType(base = ColumnStore.class, id = "STRING_ENCODED")
public class EncodedStringStore implements StringStore {

	@Nonnull
	protected DictionaryStore subType;
	@NonNull
	private Encoding encoding;

	/**
	 * Cache element lookups and as they might be time-consuming, when a trie traversal is necessary (See {@link com.bakdata.conquery.util.dict.SuccinctTrie}).
	 */
	@JsonIgnore
	private final LoadingCache<Integer, String> elementCache;


	@JsonCreator
	public EncodedStringStore(DictionaryStore subType, Encoding encoding) {
		super();
		this.subType = subType;
		this.encoding = encoding;
		elementCache = CacheBuilder.newBuilder()
								   .softValues()
								   .build(new CacheLoader<Integer, String>() {
									   @Override
									   @ParametersAreNonnullByDefault
									   public String load(Integer key) throws Exception {
										   return encoding.decode(subType.getElement(key));
									   }
								   });
	}



	@Override
	@SneakyThrows
	public String getElement(int value) {
		return elementCache.get(value);
	}

	@Override
	public int getLines() {
		return subType.getLines();
	}

	@Override
	public String createScriptValue(int event) {
		return getElement(getString(event));
	}


	@Override
	public int size() {
		return subType.size();
	}

	@Override
	public Stream<String> iterateValues() {
		return getSubType().iterateStrings().map(encoding::decode);
	}

	@Override
	public int getId(String value) {
		// Make sure we can even decode before doing so
		if (!encoding.canEncode(value)) {
			return -1;
		}

		return subType.getId(encoding.encode(value));
	}

	@Override
	public String toString() {
		return "StringTypeEncoded(encoding=" + encoding + ", subType=" + subType + ")";
	}

	@Override
	public long estimateEventBits() {
		return subType.estimateEventBits();
	}

	@Override
	public long estimateMemoryConsumptionBytes() {
		return subType.estimateMemoryConsumptionBytes();
	}

	@Override
	public long estimateTypeSizeBytes() {
		return subType.estimateTypeSizeBytes();
	}


	@Override
	public Dictionary getUnderlyingDictionary() {
		return subType.getDictionary();
	}

	@Override
	public void setUnderlyingDictionary(Dictionary dictionary) {
		subType.setDictionary(dictionary);
	}

	@Override
	public boolean isDictionaryHolding() {
		return true;
	}

	@Override
	public void setIndexStore(IntegerStore newType) {
		subType.setIndexStore(newType);
	}

	@Override
	public EncodedStringStore select(int[] starts, int[] length) {
		return new EncodedStringStore(subType.select(starts, length), getEncoding());
	}

	@Override
	public EncodedStringStore createDescription() {
		return new EncodedStringStore((DictionaryStore) subType.createDescription(), getEncoding());
	}

	@Override
	public void setString(int event, int value) {
		subType.set(event, value);
	}

	@Override
	public void setNull(int event) {
		subType.setNull(event);
	}

	@Override
	public int getString(int event) {
		return subType.getString(event);
	}

	@Override
	public boolean has(int event) {
		return subType.has(event);
	}

	/**
	 * We use common Encodings in the reversed way. What the encoding sees as "encoded" data,
	 * is actually our raw data. On this raw data the decoding of the chosen encoding applied, which
	 * yield a smaller representation for storage in the memory.
	 * <p>
	 * To use this technique all string in the dictionary must only use the dictionary that is inherent
	 * to the chosen encoding.
	 */
	@RequiredArgsConstructor
	public static enum Encoding {
		// Order is for precedence, least specific encodings go last.
		Base16LowerCase(2, BaseEncoding.base16().lowerCase().omitPadding()),
		Base16UpperCase(2, BaseEncoding.base16().upperCase().omitPadding()),
		Base32LowerCase(8, BaseEncoding.base32().lowerCase().omitPadding()),
		Base32UpperCase(8, BaseEncoding.base32().upperCase().omitPadding()),
		Base32HexLowerCase(8, BaseEncoding.base32Hex().lowerCase().omitPadding()),
		Base32HexUpperCase(8, BaseEncoding.base32Hex().upperCase().omitPadding()),
		Base64(4, BaseEncoding.base64().omitPadding()),

		UTF8(1, null) {
			@Override
			public String decode(byte[] bytes) {
				return new String(bytes, StandardCharsets.UTF_8);
			}

			@Override
			public byte[] encode(String chars) {
				return chars.getBytes(StandardCharsets.UTF_8);
			}

			@Override
			public boolean canEncode(String chars) {
				return true;
			}
		};

		private final int requiredLengthBase;
		private final BaseEncoding encoding;

		public String decode(byte[] bytes) {
			// Using encode here is valid see comment on this enum
			return encoding.encode(bytes);
		}

		public boolean canEncode(String chars) {
			// Using canDecode here is valid see comment on this enum
			return encoding.canDecode(chars)
				   && chars.length() % requiredLengthBase == 0;
		}

		public byte[] encode(String chars) {
			// Using decode here is valid see comment on this enum
			return encoding.decode(chars);
		}

	}
}
