package com.bakdata.conquery.models.types.specific;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.io.BaseEncoding;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@CPSType(base = CType.class, id = "STRING_ENCODED")
public class StringTypeEncoded<SUB extends CType<Integer,Number>&IBytesType> extends AChainedByteType<SUB> implements IStringType {

	@NonNull
	private Encoding encoding;
	
	@JsonCreator
	public StringTypeEncoded(SUB subType, Encoding encoding) {
		super(subType);
		this.encoding = encoding;
	}
	
	@Override
	public String getElement(int value) {
		return encoding.encode(subType.getElement(value));
	}
	
	@Override
	public String createScriptValue(Number value) {
		return encoding.encode(subType.getElement(value));
	}
	
	@RequiredArgsConstructor
	public static enum Encoding {
		// Order is for precedence, least specific encodings go last.
		Base16LowerCase(2,BaseEncoding.base16().lowerCase().omitPadding()),
		Base16UpperCase(2,BaseEncoding.base16().upperCase().omitPadding()),
		Base32LowerCase(8,BaseEncoding.base32().lowerCase().omitPadding()),
		Base32UpperCase(8,BaseEncoding.base32().upperCase().omitPadding()),
		Base32HexLowerCase(8,BaseEncoding.base32Hex().lowerCase().omitPadding()),
		Base32HexUpperCase(8,BaseEncoding.base32Hex().upperCase().omitPadding()),
		Base64(4, BaseEncoding.base64().omitPadding()),
		UTF8(1,null) {
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
}
