package com.bakdata.conquery.util.io;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.experimental.UtilityClass;


@UtilityClass
public class FileUtil {
	public static final Pattern SAVE_FILENAME_REPLACEMENT_MATCHER = Pattern.compile("[^a-zA-Z0-9äÄöÖüÜß .\\-]");


    public static String makeSafeFileName(String label) {
        return SAVE_FILENAME_REPLACEMENT_MATCHER.matcher(label).replaceAll("_");
    }

    public void deleteRecursive(Path path) throws IOException {
        Files.walkFileTree((path),
						   new SimpleFileVisitor<>() {
							   @Override
							   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
								   Files.delete(dir);
								   return FileVisitResult.CONTINUE;
							   }

							   @Override
							   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								   Files.delete(file);
								   return FileVisitResult.CONTINUE;
                    }
                });
    }


	/**
	 * Checks if the provided file is gzipped.
	 *
	 * @param file The file to check.
	 * @return True if it was gzipped.
	 */
	public static boolean isGZipped(@NonNull File file) {
		return file.getName().endsWith(".csv.gz");
	}

	/**
	 * Takes a {@link URI} which acts as base uri and a second uri that is more specific and resolves them.
	 * Then the resolved/resulting uri is checked if it is still a valid {@link java.net.URL}.
	 *
	 * @param baseUri     the base url. Can be null, in this case the specificUri must be a valid URL
	 * @param specificUri the specific uri. Can also be a valid url, in this case baseUri is not regarded during resolving.
	 * @return IllegalArgumentException if the resulting url is not valid.
	 */
	public static URI getResolvedUri(@Nullable URI baseUri, URI specificUri) {
		final URI resolvedURI = baseUri == null ? specificUri : baseUri.resolve(specificUri);

		try {
			// Check if resolved URI is still a valid URL
			resolvedURI.toURL();
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException("Resolved url is not valid", e);
		}
		return resolvedURI;
	}
}
