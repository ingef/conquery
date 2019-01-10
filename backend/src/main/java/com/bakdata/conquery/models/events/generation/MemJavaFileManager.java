package com.bakdata.conquery.models.events.generation;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

public class MemJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
	private final MemClassLoader classLoader;

	public MemJavaFileManager(JavaCompiler compiler, MemClassLoader classLoader) {
		super(compiler.getStandardFileManager(null, null, null));

		this.classLoader = classLoader;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
		MemJavaFileObject fileObject = new MemJavaFileObject(className);
		classLoader.addClassFile(fileObject);
		return fileObject;
	}

}
