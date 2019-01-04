package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.google.common.io.BaseEncoding;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@CPSType(base = CType.class, id = "STRING_ENCODED") @ToString
public class StringTypeEncoded extends StringType implements IStringType {

	@NonNull @Getter @ToString.Include
	private Encoding encoding;

	public StringTypeEncoded(Encoding encoding, long lines, long nullLines) {
		super();

		this.encoding = encoding;
		this.setLines(lines);
		this.setNullLines(nullLines);
	}

	@Override
	public String createScriptValue(Integer value) {
		return encoding.encode(super.getDictionary().getElementBytes(value));
	}

	@Override
	public Integer transformFromMajorType(StringType majorType, Object from) {
		Integer id = (Integer) from;
		String value = majorType.createScriptValue(id);

		return super.getDictionary().add(encoding.decode(value));
	}

	@Override
	public CType<?, StringType> bestSubType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer parse(String v) throws ParsingException {
		throw new UnsupportedOperationException();
	}

	public static enum Encoding {
		// Order is for precedence, least specific encodings go last.

		Base16LowerCase(BaseEncoding.base16().lowerCase()),
		Base16UpperCase(BaseEncoding.base16().upperCase()),
		Base32LowerCase(BaseEncoding.base32().lowerCase()),
		Base32UpperCase(BaseEncoding.base32().upperCase()),
		Base32HexLowerCase(BaseEncoding.base32Hex().lowerCase()),
		Base32HexUpperCase(BaseEncoding.base32Hex().upperCase()),
		Base64(BaseEncoding.base64());

		@Getter
		private final BaseEncoding encoding;

		private Encoding(BaseEncoding encoding) {
			this.encoding = encoding;
		}

		public String encode(byte[] bytes) {
			return getEncoding().encode(bytes);
		}

		public boolean canDecode(CharSequence chars) {
			return getEncoding().canDecode(chars);
		}

		public byte[] decode(CharSequence chars) {
			return getEncoding().decode(chars);
		}

	}
}
