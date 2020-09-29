package com.bakdata.conquery.models.events.generation;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class MemJavaFileObject extends SimpleJavaFileObject {
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final String className;
	private byte[] bytes;

	public MemJavaFileObject(String className) {
		super(URI.create("file:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
		this.className = className;
	}
	
	
	public MemJavaFileObject(String className, byte[] bytes) {
		this(className);
		this.bytes = bytes;
	}

	public String getClassName() {
		return className;
	}

	public byte[] getClassBytes() {
		if(bytes == null) {
			bytes = baos.toByteArray();
			baos = null;
		}
		return bytes;
	}

	@Override
	public OutputStream openOutputStream() {
		return baos;
	}
}