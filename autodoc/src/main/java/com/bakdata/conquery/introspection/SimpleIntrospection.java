package com.bakdata.conquery.introspection;

import java.io.File;

import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleIntrospection implements Introspection {

	private final File file;
	
	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public String getExample() {
		return "";
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public String getLine() {
		return null;
	}

	@Override
	public Introspection findField(FieldInfo field) {
		return this;
	}

	@Override
	public Introspection findMethod(MethodInfo method) {
		return this;
	}
}
