package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryClassGenerator extends ClassGenerator {

	private final MemClassLoader classLoader = new MemClassLoader();
	private final List<JavaFileObject> files = new ArrayList<>();
	private final List<String> generated = new ArrayList<>();

	@Override
	public Class<?> getClassByName(String fullClassName) throws ClassNotFoundException {
		return Class.forName(fullClassName,true, classLoader);
	}

	@Override
	protected void doCompile(JavaCompiler compiler, StandardJavaFileManager fileManager) throws IOException, URISyntaxException {
		try (JavaFileManager myFM = new MemJavaFileManager(compiler, classLoader)) {
			StringWriter output = new StringWriter();
			CompilationTask task = compiler.getTask(output, myFM, null, Arrays.asList("-g:none"), null, files);
			
			if (!task.call()) {
				throw new IllegalStateException("Failed to compile: "+output);
			}
		}
	}

	@Override
	protected void addTask(String fullClassName, String content) {
		files.add(new StringJavaFileObject(fullClassName, content));
		generated.add(fullClassName);
	}
}
