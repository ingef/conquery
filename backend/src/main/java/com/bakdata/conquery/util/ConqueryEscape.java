package com.bakdata.conquery.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.stream.IntStream;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;

/**
 * We use a custom escaping for the parts an {@link com.bakdata.conquery.models.identifiable.ids.IId} consists of.
 * This way, we ensure, that the {@link com.bakdata.conquery.models.identifiable.ids.IId#JOIN_CHAR} is not confused and that the resulting id
 * can be used safely in a URL path or query without further encoding.
 */
@UtilityClass
public class ConqueryEscape {

	private static final byte ESCAPER = '$';

	private static final BitSet dontNeedEncoding;


	static {
		// adapted from java.net.UrlEncoder
		dontNeedEncoding = new BitSet(256);

		IntStream.range('a', 'z').forEach(dontNeedEncoding::set);
		IntStream.range('A', 'Z').forEach(dontNeedEncoding::set);
		IntStream.range('0', '9').forEach(dontNeedEncoding::set);

		// We have only a few characters that won't encode unlike UrlEncode
		dontNeedEncoding.set('-');
		dontNeedEncoding.set('_');

		// Ensure that occurrences of the escaper are escaped
		assert !dontNeedEncoding.get(ESCAPER);

	}

	public static String escape(@NonNull String word) {
		if (word.isEmpty()) {
			return word;
		}


		// start escaping if a caracter is found that needs escaping
		for (int i = 0; i < word.length(); i++) {
			final char c = word.charAt(i);

			// Check if the character is larger than a byte or if that byte needs encoding
			if (((c >> 8) & 0xFF) != 0 || !dontNeedEncoding((byte) (c & 0x00FF))) {
				// Convert it to the encoding we expect to consume
				byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
				// Prepare the buffer for the encoded word
				ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length + 5);
				// Since we encountered the first character that needs encoding, we can assume
				// that until here the byte index and the char index point to the same character
				baos.write(bytes, 0, i);

				// Then write the rest and escape
				return escapeRequired(bytes, i, baos);
			}
		}

		return word;
	}


	private static String escapeRequired(byte[] bytes, int index, ByteArrayOutputStream baos) {
		for (int i = index; i < bytes.length; i++) {
			if (dontNeedEncoding(bytes[i])) {
				baos.write(bytes[i]);
			}
			else {
				encode(bytes[i], baos);
			}
		}
		return baos.toString(StandardCharsets.US_ASCII);
	}
	
	public static String unescape(@NonNull String word) {
		if(word.isEmpty()) {
			return word;
		}
		
		byte[] bytes = word.getBytes(StandardCharsets.US_ASCII);
		if(!ArrayUtils.contains(bytes, ESCAPER)) {
			return word;
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
		
		for(int i=0;i<bytes.length;i++) {
			if(bytes[i] == ESCAPER) {
				i += decode(bytes, i, out);
			}
			else {
				out.write(bytes[i]);
			}
		}

		return out.toString(StandardCharsets.UTF_8);
	}

	private static void encode(byte b, ByteArrayOutputStream out) {
		out.write(ESCAPER);
		out.write(Character.forDigit((b >> 4) & 0xF, 16));
		out.write(Character.forDigit((b & 0xF), 16));
	}

	private static int decode(byte[] bytes, int i, ByteArrayOutputStream out) {
		out.write((Character.digit(bytes[i + 1], 16) << 4) + Character.digit(bytes[i + 2], 16));
		return 2;
	}

	private static boolean dontNeedEncoding(byte v) {
		return v >= 0 && dontNeedEncoding.get(v);
	}

}
