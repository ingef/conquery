package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryClassGenerator extends ClassGenerator {

	private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager FILE_MANAGER = new CachedJavaFileManager(COMPILER.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8));
	
	private final MemClassLoader classLoader;
	private final List<JavaFileObject> files = new ArrayList<>();

	public MemoryClassGenerator() throws IOException {
		classLoader = new MemClassLoader();
	}

	@Override
	public Class<?> getClassByName(String fullClassName) throws ClassNotFoundException {
		return Class.forName(fullClassName,true, classLoader);
	}
	
	@Override
	public void doCompile() throws IOException {
		synchronized (COMPILER) {
			try (JavaFileManager fileManager = new MemJavaFileManager(
				FILE_MANAGER, 
				classLoader)
			) {
				StringWriter output = new StringWriter();
				CompilationTask task = COMPILER.getTask(output, fileManager, null, Arrays.asList("-g:none"), null, files);
				
				if (!task.call()) {
					throw new IllegalStateException("Failed to compile: "+output);
				}
			}
		}
	}

	@Override
	protected void addTask(String fullClassName, String content) {
		files.add(new StringJavaFileObject(fullClassName, content));
	}
}
