package com.bakdata.conquery.models.events.generation;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

class MemJavaFileObject extends SimpleJavaFileObject {
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final String className;

	public MemJavaFileObject(String className) {
		super(URI.create("file:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

	public byte[] getClassBytes() {
		return baos.toByteArray();
	}

	@Override
	public OutputStream openOutputStream() {
		return baos;
	}
}