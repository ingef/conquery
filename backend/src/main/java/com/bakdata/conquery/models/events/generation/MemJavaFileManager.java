package com.bakdata.conquery.models.events.generation;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

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
