package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.AllArgsConstructor;
import lombok.Data;

public class CachedJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

	private LoadingCache<ListKey, Iterable<JavaFileObject>> cache = CacheBuilder.newBuilder()
		.expireAfterAccess(1, TimeUnit.HOURS)
		.build(new CacheLoader<ListKey, Iterable<JavaFileObject>>() {
			@Override
			public Iterable<JavaFileObject> load(ListKey key) throws Exception {
				return CachedJavaFileManager.super.list(key.getLocation(), key.getPackageName(), key.getKinds(), key.isRecurse());
			}
		});
	
	protected CachedJavaFileManager(JavaFileManager fileManager) {
		super(fileManager);
	}

	
	@Override
	public synchronized Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
		ListKey key = new ListKey(location, packageName, kinds, recurse);
		try {
			return cache.get(key);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	@Data
	@AllArgsConstructor
	private static class ListKey {
		private final Location location;
		private final String packageName;
		private final Set<Kind> kinds;
		private final boolean recurse;
	}
}
