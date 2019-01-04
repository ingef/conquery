package com.bakdata.conquery.models.events.generation;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.apache.commons.lang3.StringUtils;

import com.github.powerlibraries.io.Out;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DebugClassGenerator extends ClassGenerator {

	private File tmp;
	private final URLClassLoader classLoader;
	private final List<File> files = new ArrayList<>();
	private final List<JavaFileObject> mFiles = new ArrayList<>();
	private final List<String> generated = new ArrayList<>();


	public DebugClassGenerator() throws IOException {
		log.info("Started code generator in Debug Mode!");
		tmp = new File("codeGenDebug");
		classLoader = new URLClassLoader(new URL[] { tmp.toURI().toURL() }, Thread.currentThread().getContextClassLoader());
		log.debug("Generating Java classes in {}", tmp.getAbsolutePath());
	}

	@Override
	public void addTask(String fullClassName, String content) throws IOException {
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
	public Class<?> getClassByName(String fullClassName) throws ClassNotFoundException {
		return Class.forName(fullClassName, true, classLoader);
	}

	@Override
	public void compile() throws IOException, URISyntaxException {
		try (StandardJavaFileManager fileManager = getCompiler().getStandardFileManager(null, null, null)) {
			Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(files);
			StringWriter output = new StringWriter();
			CompilationTask task = getCompiler().getTask(output, fileManager, null, Arrays.asList("-g", "-Xlint"), null, units);
			
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
}
