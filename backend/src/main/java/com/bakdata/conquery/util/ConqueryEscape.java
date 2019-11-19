package com.bakdata.conquery.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.ArrayUtils;

import lombok.NonNull;


public class ConqueryEscape {
	
	private static final byte ESCAPER = '$';
	private static final ConqueryEscape INSTANCE = new ConqueryEscape();

	public static String escape(@NonNull String word) {
		return INSTANCE.escapeString(word);
	}
		
	protected String escapeString(String word) {
		if(word.isEmpty()) {
			return word;
		}
		
		byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
		
		//if the first does not match we escape everything
		if(!matchesFirst(bytes[0])) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length + 5);
			encode(bytes[0], baos);
			return escapeRequired(bytes, 1, baos);
		}
		
		//if the first matched we walk through the rest and check if any don't match the allowed characters
		for(int i=1;i<bytes.length;i++) {
			if(!matchesOther(bytes[i])) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length + 5);
				baos.write(bytes, 0, i);
				return escapeRequired(bytes, i, baos);
			}
		}
		
		return word;
	}
	
			
	private String escapeRequired(byte[] bytes, int index, ByteArrayOutputStream baos) {
		for(int i=index;i<bytes.length;i++) {
			if(matchesOther(bytes[i])) {
				baos.write(bytes[i]);
			}
			else {
				encode(bytes[i], baos);
			}
		}
		return new String(baos.toByteArray(), StandardCharsets.US_ASCII);
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
		
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}
	
	private void encode(byte b, ByteArrayOutputStream out) {
		out.write(ESCAPER);
		out.write(Character.forDigit((b >> 4) & 0xF, 16));
		out.write(Character.forDigit((b & 0xF), 16));
	}
	
	private int decode(byte[] bytes, int i, ByteArrayOutputStream out) {
		out.write((Character.digit(bytes[i+1], 16) << 4) + Character.digit(bytes[i+2], 16));
		return 2;
	}

	protected boolean matchesOther(byte v) {
		return v>=(byte)'!' && v<=(byte)'~' && v!=ESCAPER && v!=(byte)'.';
	}

	protected boolean matchesFirst(byte v) {
		return matchesOther(v);
	}
}
