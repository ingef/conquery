package com.bakdata.conquery.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import lombok.NonNull;


public class ConqueryEscape {
	
	private static final byte ESCAPER = '$';

	public static String escape(@NonNull String word) {
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
	
			
	private static String escapeRequired(byte[] bytes, int index, ByteArrayOutputStream baos) {
		for(int i=index;i<bytes.length;i++) {
			if(matchesOther(bytes[i])) {
				baos.write(bytes[i]);
			}
			else {
				encode(bytes[i], baos);
			}
		}
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}
	
	public static String unescape(@NonNull String word) {
		if(word.isEmpty()) {
			return word;
		}
		
		byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
		
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
	
	private static void encode(byte b, ByteArrayOutputStream out) {
		out.write(ESCAPER);
		out.write(Character.forDigit((b >> 4) & 0xF, 16));
		out.write(Character.forDigit((b & 0xF), 16));
	}
	
	private static int decode(byte[] bytes, int i, ByteArrayOutputStream out) {
		out.write((Character.digit(bytes[i+1], 16) << 4) + Character.digit(bytes[i+2], 16));
		return 2;
	}

	private static boolean matchesOther(byte v) {
		return matchesFirst(v) || (v>=(byte)'0' && v<=(byte)'9') || v == '_';
	}

	private static boolean matchesFirst(byte v) {
		return (v>=(byte)'a' && v<=(byte)'z') || (v>=(byte)'A' && v<=(byte)'Z');
	}
}
