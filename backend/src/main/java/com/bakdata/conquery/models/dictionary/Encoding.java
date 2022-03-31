package com.bakdata.conquery.models.dictionary;

import java.nio.charset.StandardCharsets;

import com.google.common.io.BaseEncoding;
import lombok.RequiredArgsConstructor;

/**
 * We use common Encodings in the reversed way. What the encoding sees as "encoded" data,
 * is actually our raw data. On this raw data the decoding of the chosen encoding applied, which
 * yield a smaller representation for storage in the memory.
 * <p>
 * To use this technique all string in the dictionary must only use the dictionary that is inherent
 * to the chosen encoding.
 */
@RequiredArgsConstructor
public enum Encoding {
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
