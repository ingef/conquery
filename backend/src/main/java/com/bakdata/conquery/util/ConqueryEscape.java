package com.bakdata.conquery.util;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;


public class ConqueryEscape {

	private static final char URL_ESCAPE_CODE_PREFIX = '%';
	private static final String IID_JOIN_CHAR_URL_ESCAPED;

	static {
		IID_JOIN_CHAR_URL_ESCAPED = urlEscapeChar(IId.JOIN_CHAR);
		assert (!IID_JOIN_CHAR_URL_ESCAPED.contains(String.valueOf(IId.JOIN_CHAR)));
	}

	/**
	 * We explicitly escape the IId.JOIN_CHAR so that the parts of a serialized ID don't contain it.
	 */
	public static String escape(@NonNull String word) {
		final String encode = URLEncoder.encode(word, StandardCharsets.UTF_8);
		return encode.replace(String.valueOf(IId.JOIN_CHAR), IID_JOIN_CHAR_URL_ESCAPED);
	}

	public static String unescape(@NonNull String word) {
		return URLDecoder.decode(word, StandardCharsets.UTF_8);
	}

	private static final byte ESCAPER = '$';

	public static String oldUnescape(@NonNull String word) {
		if (word.isEmpty()) {
			return word;
		}

		byte[] bytes = word.getBytes(StandardCharsets.US_ASCII);
		if (!ArrayUtils.contains(bytes, ESCAPER)) {
			return word;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);

		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == ESCAPER) {
				i += decode(bytes, i, out);
			}
			else {
				out.write(bytes[i]);
			}
		}

		return out.toString(StandardCharsets.UTF_8);
	}

	private static int decode(byte[] bytes, int i, ByteArrayOutputStream out) {
		out.write((Character.digit(bytes[i + 1], 16) << 4) + Character.digit(bytes[i + 2], 16));
		return 2;
	}

	private static String urlEscapeChar(char c) {
		Byte[] buf = new Byte[2];
		buf[0] = (byte) (c >>> 8);
		buf[1] = (byte) (c & 0x00FF);
		return Arrays.stream(buf)
					 .dropWhile(b -> b == 0)
					 .map(b -> String.format("%s%X", URL_ESCAPE_CODE_PREFIX, b))
					 .collect(Collectors.joining());
	}
}
