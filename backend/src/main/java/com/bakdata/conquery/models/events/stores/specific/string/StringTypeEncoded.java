package com.bakdata.conquery.models.events.stores.specific.string;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 *
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_ENCODED")
public class StringTypeEncoded implements StringStore {

	@Nonnull
	protected StringTypeDictionary subType;
	@NonNull
	private Encoding encoding;

	@JsonCreator
	public StringTypeEncoded(StringTypeDictionary subType, Encoding encoding) {
		super();
		this.subType = subType;
		this.encoding = encoding;
	}

	@Override
	public String getElement(int value) {
		return encoding.encode(subType.getElement(value));
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
	public void loadDictionaries(NamespacedStorage storage) {
		subType.loadDictionaries(storage);
	}


	@Override
	public int size() {
		return subType.size();
	}

	@Override
	public int getId(String value) {
		return subType.getId(encoding.decode(value));
	}

	@Override
	public Iterator<String> iterator() {
		Iterator<byte[]> subIt = subType.iterator();
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return subIt.hasNext();
			}

			@Override
			public String next() {
				return encoding.encode(subIt.next());
			}
		};
	}

	@Override
	public String toString() {
		return "StringTypeEncoded[encoding=" + encoding + ", subType=" + subType + "]";
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
	public void setUnderlyingDictionary(DictionaryId newDict) {
		subType.setUnderlyingDictionary(newDict);
	}

	@Override
	public void setIndexStore(IntegerStore newType) {
		subType.setIndexStore(newType);
	}

	@Override
	public StringTypeEncoded select(int[] starts, int[] length) {
		return new StringTypeEncoded(subType.select(starts, length), getEncoding());
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
			public String encode(byte[] bytes) {
				return new String(bytes, StandardCharsets.UTF_8);
			}

			@Override
			public byte[] decode(String chars) {
				return chars.getBytes(StandardCharsets.UTF_8);
			}

			@Override
			public boolean canDecode(String chars) {
				return true;
			}
		};

		private final int requiredLengthBase;
		private final BaseEncoding encoding;

		public String encode(byte[] bytes) {
			return encoding.encode(bytes);
		}

		public boolean canDecode(String chars) {
			return encoding.canDecode(chars)
				   && chars.length() % requiredLengthBase == 0;
		}

		public byte[] decode(String chars) {
			return encoding.decode(chars);
		}

	}
}
