package com.bakdata.conquery.models.events.generation;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.ToolProvider;

import com.bakdata.conquery.util.DebugMode;

import lombok.Getter;

public abstract class ClassGenerator implements Closeable {
	@Getter
	protected final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	protected JavaFileManager fileManager;
	private final List<String> generated = new ArrayList<>();


	public static ClassGenerator create() throws IOException {
		if (DebugMode.isActive()) {
			return new DebugClassGenerator();
		}
		else {
			return new MemoryClassGenerator();
		}
	}
	
	public ClassGenerator() {
		this.fileManager = compiler.getStandardFileManager(null, null, null);
	}

	public void addForCompile(String fullClassName, String content) throws IOException, ClassNotFoundException {
		generated.add(fullClassName);
		addTask(fullClassName, content);
	}

	protected abstract void addTask(String fullClassName, String content) throws IOException;

	public abstract Class<?> getClassByName(String fullClassName) throws ClassNotFoundException;

	public abstract void compile() throws IOException, URISyntaxException;

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
		fileManager.close();
	}
}
