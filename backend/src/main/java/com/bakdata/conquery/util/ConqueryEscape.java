package com.bakdata.conquery.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.NonNull;
import org.apache.commons.codec.binary.Hex;


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

	private static String urlEscapeChar(char c) {
		byte[] buf = new byte[2];
		buf[0] = (byte) ((c & 0xFF00) >> 8);
		buf[1] = (byte) (c & 0x00FF);
		final String s = URL_ESCAPE_CODE_PREFIX + Hex.encodeHexString(buf).replaceFirst("^0*", "");

		return s;
	}
}
