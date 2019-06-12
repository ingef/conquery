package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.util.DebugMode;

public abstract class ClassGenerator {
	private final List<String> generated = new ArrayList<>();


	public static ClassGenerator create() throws IOException {
		if (DebugMode.isActive()) {
			return new DebugClassGenerator();
		}
		else {
			return new MemoryClassGenerator();
		}
	}
	
	public void addForCompile(String fullClassName, String content) throws IOException, ClassNotFoundException {
		generated.add(fullClassName);
		addTask(fullClassName, content);
	}

	protected abstract void addTask(String fullClassName, String content) throws IOException;

	public abstract Class<?> getClassByName(String fullClassName) throws ClassNotFoundException;

	public abstract void compile() throws IOException, URISyntaxException;
}
