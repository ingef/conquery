package com.bakdata.conquery.util.io;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

import com.google.auto.service.AutoService;

/**
 * Allows parsing URLs that start with "classpath://" to access resources on the classpath. The actual access to the resource is done using
 * {@link ClassPathHandler#openConnection(URL)} or through the {@link ClassPathFileSystemProvider}, when the URL is converted to a URI.
 */
@AutoService(URLStreamHandlerProvider.class)
public class ClassPathURLStreamHandlerProvider extends URLStreamHandlerProvider {
	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ("classpath".equals(protocol)) {
			return new ClassPathHandler();
		}
		return null;
	}
}
