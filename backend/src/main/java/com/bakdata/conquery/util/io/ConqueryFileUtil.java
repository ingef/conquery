package com.bakdata.conquery.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConqueryFileUtil {
	
	public static final Path TEMPORARY_DIRECTORY;
	
	static {
		try {
			TEMPORARY_DIRECTORY = Files.createTempDirectory("conquery_");
			TEMPORARY_DIRECTORY.toFile().deleteOnExit();
		} catch (IOException e) {
			throw new Error("Could not create temporary directory", e);
		}
	}

	public static File createTempFile(String prefix, String extension) throws IOException {
		File f = Files.createTempFile(TEMPORARY_DIRECTORY, prefix, "." + extension).toFile();
		f.deleteOnExit();
		return f;
	}
}
