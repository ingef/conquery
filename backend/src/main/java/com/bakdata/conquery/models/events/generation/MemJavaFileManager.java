package com.bakdata.conquery.models.events.generation;

import java.io.IOException;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class MemJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private final MemClassLoader classLoader;

	public MemJavaFileManager(JavaFileManager fileManager, MemClassLoader classLoader) {
		super(fileManager);

		this.classLoader = classLoader;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
		MemJavaFileObject fileObject = new MemJavaFileObject(className);
		classLoader.addClassFile(fileObject);
		return fileObject;
	}

	@Override
	public void close() throws IOException {
		super.flush();
		//super.close();
	}
}
