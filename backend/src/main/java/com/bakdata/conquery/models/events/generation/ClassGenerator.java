package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.concurrent.BasicFuture;

import com.bakdata.conquery.util.DebugMode;

public abstract class ClassGenerator {
	
	private static final ConcurrentHashMap<Set<Pair<String,String>>, Future<Map<String, Class<?>>>> cache = new ConcurrentHashMap<>();
	
	private final List<String> tasks = new ArrayList<>();
	private final List<String> contents = new ArrayList<>();


	public static ClassGenerator create() throws IOException {
		if (DebugMode.isActive()) {
			return new DebugClassGenerator();
		}
		else {
			return new MemoryClassGenerator();
		}
	}

	public void addForCompile(String fullClassName, String content) throws IOException, ClassNotFoundException {
		tasks.add(fullClassName);
		contents.add(content);
	}

	protected abstract void addTask(String fullClassName, String content) throws IOException;

	protected abstract Class<?> getClassByName(String fullClassName) throws ClassNotFoundException;

	public Map<String, Class<?>> compile() throws IOException, URISyntaxException {
		try {
			Set<Pair<String,String>> taskSet = new HashSet<>();
			for(int i = 0;i<tasks.size();i++) {
				taskSet.add(Pair.of(tasks.get(i), contents.get(i)));
			}
			BasicFuture<Map<String, Class<?>>> future = new BasicFuture<>(null);
			Future<Map<String, Class<?>>> oldValue = cache.putIfAbsent(taskSet, future);
			
			//if we already have somebody working on it
			if(oldValue != null) {
				return oldValue.get();
			}
	
			for(int i = 0;i<tasks.size();i++) {
				addTask(tasks.get(i), contents.get(i));
			}
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
				doCompile(compiler, fileManager);
				Map<String, Class<?>> map = new HashMap<>();
				// load classes to memory before closing
				for (String task : tasks) {
					try {
						map.put(task, getClassByName(task));
					} catch (ClassNotFoundException e) {
						throw new IllegalStateException("Failed to load class that was generated " + task, e);
					}
				}
				future.completed(map);
			}
			return future.get();
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to get classes from parallel compilation ", e);
		}
	}

	protected abstract void doCompile(JavaCompiler compiler, StandardJavaFileManager fileManager) throws IOException, URISyntaxException;
}
