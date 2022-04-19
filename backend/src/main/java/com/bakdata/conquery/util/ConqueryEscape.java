package com.bakdata.conquery.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import org.apache.commons.lang3.ArrayUtils;

import lombok.NonNull;


public class ConqueryEscape {

	private static final byte ESCAPER = '$';
	private static final ConqueryEscape INSTANCE = new ConqueryEscape();

	static BitSet dontNeedEncoding;


	static {
		// from java.net.UrlEncoder
		dontNeedEncoding = new BitSet(256);
		int i;
		for (i = 'a'; i <= 'z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = 'A'; i <= 'Z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = '0'; i <= '9'; i++) {
			dontNeedEncoding.set(i);
		}
		dontNeedEncoding.set(' '); /* encoding a space to a + is done
		 * in the encode() method */
		dontNeedEncoding.set('-');
		dontNeedEncoding.set('_');
		dontNeedEncoding.set('*');

		// Ensure that occurrences of the escaper are escaped
		assert !dontNeedEncoding.get(ESCAPER);

	}

	public static String escape(@NonNull String word) {
		return INSTANCE.escapeString(word);
	}

	protected String escapeString(String word) {
		if (word.isEmpty()) {
			return word;
		}

		byte[] bytes = word.getBytes(StandardCharsets.UTF_8);

		// start escaping if a caracter is found that needs escaping
		for (int i = 0; i < bytes.length; i++) {
			if (!dontNeedEncoding(bytes[i])) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length + 5);
				baos.write(bytes, 0, i);
				return escapeRequired(bytes, i, baos);
			}
		}

		return word;
	}
	
			
	private String escapeRequired(byte[] bytes, int index, ByteArrayOutputStream baos) {
		for(int i=index;i<bytes.length;i++) {
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
		return INSTANCE.unescapeString(word);
	}
	
	protected String unescapeString(String word) {
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
	
	private void encode(byte b, ByteArrayOutputStream out) {
		out.write(ESCAPER);
		out.write(Character.forDigit((b >> 4) & 0xF, 16));
		out.write(Character.forDigit((b & 0xF), 16));
	}

	private int decode(byte[] bytes, int i, ByteArrayOutputStream out) {
		out.write((Character.digit(bytes[i + 1], 16) << 4) + Character.digit(bytes[i + 2], 16));
		return 2;
	}

	protected boolean dontNeedEncoding(byte v) {
		return v >= 0 && dontNeedEncoding.get(v);
		//		return v>=(byte)'!'
		//			   && v<=(byte)'~'
		//			   && v!=ESCAPER
		//			   && v!=(byte)'.'
		//			   && v!=(byte)'/';
	}

}
