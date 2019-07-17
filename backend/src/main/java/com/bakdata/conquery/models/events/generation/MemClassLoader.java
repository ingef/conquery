package com.bakdata.conquery.models.events.generation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MemClassLoader extends ClassLoader {
	private final Map<String, MemJavaFileObject> classFiles = new HashMap<String, MemJavaFileObject>();

	public MemClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
	}

	public void addClassFile(MemJavaFileObject memJavaFileObject) {
		classFiles.put(memJavaFileObject.getClassName(), memJavaFileObject);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		MemJavaFileObject fileObject = classFiles.get(name);

		if (fileObject != null) {
			byte[] bytes = fileObject.getClassBytes();
			return defineClass(name, bytes, 0, bytes.length);
		}

		return super.findClass(name);
	}

	public Map<String, byte[]> getClasses() {
		return classFiles
			.entrySet()
			.stream()
			.collect(Collectors.toMap(
				Entry::getKey,
				e->e.getValue().getClassBytes()
			));
	}
}