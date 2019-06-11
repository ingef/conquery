package com.bakdata.conquery.models.events.generation;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.util.DebugMode;

public abstract class ClassGenerator implements Closeable {
	private final List<String> generated = new ArrayList<>();


	public static ClassGenerator create() throws IOException {
		if (DebugMode.isActive()) {
			return new DebugClassGenerator();
		}
		else {
			return new MemoryClassGenerator();
		}
	}
	
	public void addForCompile(String fullClassName, String content) throws IOException, ClassNotFoundException {
		generated.add(fullClassName);
		addTask(fullClassName, content);
	}

	protected abstract void addTask(String fullClassName, String content) throws IOException;

	public abstract Class<?> getClassByName(String fullClassName) throws ClassNotFoundException;

	public void compile() throws IOException, URISyntaxException {
		try {
			doCompile();
		} catch(Exception e) {
			//so that when we close the closing does not try to load any classes
			//which would mask the exceptions
			generated.clear();
			throw e;
		}
	}

	protected abstract void doCompile() throws IOException, URISyntaxException;

	@Override
	public void close() throws IOException {
		// load classes to memory before closing
		for (String cl : generated) {
			try {
				getClassByName(cl);
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Failed to load class that was generated " + cl, e);
			}
		}
	}
}
