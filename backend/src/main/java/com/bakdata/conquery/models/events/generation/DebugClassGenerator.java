package com.bakdata.conquery.models.events.generation;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.lang3.StringUtils;

import com.github.powerlibraries.io.Out;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DebugClassGenerator extends ClassGenerator {
	
	private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();

	private File tmp;
	private final URLClassLoader classLoader;
	private final List<File> files = new ArrayList<>();
	private final List<String> generated = new ArrayList<>();


	public DebugClassGenerator() throws IOException {
		log.info("Started code generator in Debug Mode!");
		tmp = new File("codeGenDebug");
		classLoader = new URLClassLoader(new URL[] { tmp.toURI().toURL() }, Thread.currentThread().getContextClassLoader());
		log.debug("Generating Java classes in {}", tmp.getAbsolutePath());
	}

	@Override
	public synchronized void addTask(String fullClassName, String content) throws IOException {
		String[] parts = StringUtils.split(fullClassName, '.');
		String className = parts[parts.length - 1];
		File dir = tmp;
		for (int i = 0; i < parts.length - 1; i++) {
			dir = new File(dir, parts[i]);
		}
		dir.mkdirs();
		File src = new File(dir, className + ".java");
		Out.file(src).withUTF8().write(content);
		files.add(src);
		generated.add(fullClassName);
	}

	@Override
	public synchronized Class<?> getClassByName(String fullClassName) throws ClassNotFoundException {
		return Class.forName(fullClassName, true, classLoader);
	}

	@Override
	public void doCompile() throws IOException {
		synchronized (COMPILER) {
			try (StandardJavaFileManager fileManager = COMPILER.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
				Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(files);
				StringWriter output = new StringWriter();
				CompilationTask task = COMPILER.getTask(output, fileManager, null, Arrays.asList("-g", "-Xlint"), null, units);
				
				if (!task.call()) {
					throw new IllegalStateException("Failed to compile: "+output);
				}
			}
		}
	}
}
