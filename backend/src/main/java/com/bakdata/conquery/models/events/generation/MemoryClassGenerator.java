package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryClassGenerator extends ClassGenerator {

	private final JavaFileManager fileManager;
	private final MemClassLoader classLoader;
	private final List<JavaFileObject> files = new ArrayList<>();
	private final List<String> generated = new ArrayList<>();

	public MemoryClassGenerator() throws IOException {
		classLoader = new MemClassLoader();
		fileManager = new MemJavaFileManager(compiler, classLoader);
	}

	@Override
	public Class<?> getClassByName(String fullClassName) throws ClassNotFoundException {
		return Class.forName(fullClassName,true, classLoader);
	}

	@Override
	public void compile() throws IOException, URISyntaxException {
		try (JavaFileManager fileManager = new MemJavaFileManager(compiler, classLoader)) {
			StringWriter output = new StringWriter();
			CompilationTask task = compiler.getTask(output, fileManager, null, Arrays.asList("-g:none"), null, files);
			
			if (!task.call()) {
				throw new IllegalStateException("Failed to compile: "+output);
			}
		}
	}

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

	@Override
	protected void addTask(String fullClassName, String content) {
		files.add(new StringJavaFileObject(fullClassName, content));
		generated.add(fullClassName);
	}
}
