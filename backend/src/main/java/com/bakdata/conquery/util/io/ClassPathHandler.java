package com.bakdata.conquery.util.io;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.github.powerlibraries.io.In;

public class ClassPathHandler extends URLStreamHandler {
	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return new URLConnection(u) {
			@Override
			public void connect() throws IOException {
				// Nothing to do
			}

			@Override
			public InputStream getInputStream() throws IOException {
				final URL resource = this.getClass().getResource(getURL().getPath());
				if (resource == null) {
					throw new FileNotFoundException(getURL().toString());
				}
				return In.resource(resource).asStream();
			}
		};
	}
}
