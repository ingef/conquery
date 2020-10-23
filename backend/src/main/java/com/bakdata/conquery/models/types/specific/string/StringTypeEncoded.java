package com.bakdata.conquery.models.types.specific.string;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_ENCODED")
public class StringTypeEncoded extends StringType {

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
	public String createScriptValue(Integer value) {
		return encoding.encode(subType.getElement(value.longValue()));
	}

	@Override
	public void init(DatasetId dataset) {
		subType.init(dataset);
	}

	@Override
	public void loadExternalInfos(NamespacedStorage storage) {
		subType.loadExternalInfos(storage);
	}


	@Override
	public void storeExternalInfos(Consumer<Dictionary> dictionaryConsumer) {
		subType.storeExternalInfos(dictionaryConsumer);
	}

	@Override
	public void readHeader(JsonParser input) throws IOException {
		subType.readHeader(input);
	}

	@Override
	public void writeHeader(OutputStream out) throws IOException {
		subType.writeHeader(out);
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
		return new Iterator<String>() {
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
	public long estimateMemoryBitWidth() {
		return subType.estimateMemoryBitWidth();
	}

	@Override
	public long estimateMemoryConsumption() {
		return subType.estimateMemoryConsumption();
	}

	@Override
	public long estimateTypeSize() {
		return subType.estimateTypeSize();
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return subType.getDictionary();
	}

	@Override
	public void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType) {
		subType.adaptUnderlyingDictionary(newDict, newNumberType);
	}

	@Override
	public StringTypeEncoded select(int[] starts, int[] length) {
		return new StringTypeEncoded(subType.select(starts, length), getEncoding());
	}

	@Override
	public void set(int event, Integer value) {
		subType.set(event, value);
	}

	@Override
	public Integer get(int event) {
		return subType.get(event);
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
