package com.bakdata.conquery.util;

import lombok.NonNull;


public class ConqueryJavaEscape extends ConqueryEscape {
	
	private static final ConqueryJavaEscape INSTANCE = new ConqueryJavaEscape();

	public static String escape(@NonNull String word) {
		return INSTANCE.escapeString(word);
	}
	
	public static String unescape(@NonNull String word) {
		return INSTANCE.unescapeString(word);
	}
	
	@Override
	protected boolean matchesOther(byte v) {
		return matchesFirst(v) || (v>=(byte)'0' && v<=(byte)'9') || v == '_';
	}

	@Override
	protected boolean matchesFirst(byte v) {
		return (v>=(byte)'a' && v<=(byte)'z') || (v>=(byte)'A' && v<=(byte)'Z');
	}
}
