package com.bakdata.conquery.models.types.specific;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.io.BaseEncoding;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@CPSType(base = CType.class, id = "STRING_ENCODED") @ToString
public class StringTypeEncoded extends StringTypeVarInt implements IStringType {

	@NonNull @Getter @ToString.Include
	private Encoding encoding;
	
	@JsonCreator
	public StringTypeEncoded(VarIntType numberType, Encoding encoding) {
		super(numberType);
		this.encoding = encoding;
	}

	@Override
	public String createScriptValue(int value) {
		return encoding.encode(super.getDictionary().getElementBytes(value));
	}

	@Override
	public int getStringId(String string) {
		return super.getDictionary().getId(encoding.decode(string));
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
		Base64(4, BaseEncoding.base64().omitPadding());

		private final int requiredLengthBase;
		@Getter
		private final BaseEncoding encoding;

		public String encode(byte[] bytes) {
			return getEncoding().encode(bytes);
		}

		public boolean canDecode(CharSequence chars) {
			return getEncoding().canDecode(chars)
				&& chars.length() % requiredLengthBase == 0;
		}

		public byte[] decode(CharSequence chars) {
			return getEncoding().decode(chars);
		}

	}
	
	@Override
	public Iterator<String> iterator() {
		return IntStream
			.range(0, getDictionary().size())
			.mapToObj(this::createScriptValue)
			.iterator();
	}
}
