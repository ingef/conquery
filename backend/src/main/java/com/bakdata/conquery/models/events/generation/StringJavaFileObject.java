package com.bakdata.conquery.models.events.generation;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class StringJavaFileObject extends SimpleJavaFileObject {
	private final CharSequence code;

	public StringJavaFileObject(String name, CharSequence code) {
		super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
		this.code = code;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return code;
	}
}