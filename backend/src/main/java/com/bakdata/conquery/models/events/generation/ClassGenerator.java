package com.bakdata.conquery.models.events.generation;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.util.DebugMode;
import com.github.powerlibraries.io.Out;

public class ClassGenerator {
	private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager FILE_MANAGER = new CachedJavaFileManager(COMPILER.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8));
	
	private final List<String> generated = new ArrayList<>();	
	private final MemClassLoader classLoader = new MemClassLoader();
	private final List<StringJavaFileObject> files = new ArrayList<>();
	private final boolean debug = DebugMode.isActive();

	public void addForCompile(String fullClassName, String content) throws IOException, ClassNotFoundException {
		generated.add(fullClassName);
		addTask(fullClassName, content);
	}

	protected void addTask(String fullClassName, String content) throws IOException {
		files.add(new StringJavaFileObject(fullClassName, content));
		if(debug) {
			File tmp = new File("codeGenDebug");
			String[] parts = StringUtils.split(fullClassName, '.');
			String className = parts[parts.length - 1];
			File dir = tmp;
			for (int i = 0; i < parts.length - 1; i++) {
				dir = new File(dir, parts[i]);
			}
			dir.mkdirs();
			File src = new File(dir, className + ".java");
			Out.file(src).withUTF8().write(content);
		}
	}

	public Class<?> getClassByName(String fullClassName) throws ClassNotFoundException {
		return Class.forName(fullClassName,true, classLoader);
	}

	public void compile() throws IOException, URISyntaxException {
		synchronized (COMPILER) {
			try (JavaFileManager fileManager = new MemJavaFileManager(
				FILE_MANAGER, 
				classLoader)
			) {
				StringWriter output = new StringWriter();
				CompilationTask task = COMPILER.getTask(output, fileManager, null, Arrays.asList(debug?"-g":"-g:none"), null, files);
				
				if (!task.call()) {
					throw new IllegalStateException("Failed to compile: "+output);
				}
			}
		}
	}
}
