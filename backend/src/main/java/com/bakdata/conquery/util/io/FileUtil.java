package com.bakdata.conquery.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import com.bakdata.conquery.ConqueryConstants;
import lombok.NonNull;
import lombok.experimental.UtilityClass;


@UtilityClass
public class FileUtil {
    public static final Pattern SAVE_FILENAME_REPLACEMENT_MATCHER = Pattern.compile("[^a-zA-Z0-9äÄöÖüÜß \\.\\-]");


    public static String makeSafeFileName(String label, String fileExtension) {
        return SAVE_FILENAME_REPLACEMENT_MATCHER.matcher(label + "." + fileExtension).replaceAll("_");
    }

    public void deleteRecursive(Path path) throws IOException {
        Files.walkFileTree((path),
                new SimpleFileVisitor<Path>() {
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
     * @param file The file to check.
     * @return True if it was gzipped.
     * @throws IOException if an I/O error occurs.
     */
    public static boolean isGZipped(@NonNull File file) throws IOException {
        return file.getName().endsWith(".csv.gz");
    }

	/**
	 * converts the provided cqpp file to an inputstream
	 * @param file cqpp file to convert
	 * @return file as inputstream
	 * @throws IOException if any I/O error occured
	 */
    public static InputStream cqppFileToInputstream(@NonNull File file) throws IOException {

		StringJoiner errors = new StringJoiner("\n");

		if (!file.canRead()) {
			errors.add("Cannot read.");
		}

		if (!file.exists()) {
			errors.add("Does not exist.");
		}

		if (!file.isAbsolute()) {
			errors.add("Is not absolute.");
		}

		if (!file.getPath().endsWith(ConqueryConstants.EXTENSION_PREPROCESSED)) {
			errors.add(String.format("Does not end with `%s`.", ConqueryConstants.EXTENSION_PREPROCESSED));
		}

		if (errors.length() > 0) {
			throw new IOException(errors.toString());
		}

		return new FileInputStream(file);
	}

}
