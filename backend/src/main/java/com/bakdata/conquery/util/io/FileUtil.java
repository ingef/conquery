package com.bakdata.conquery.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import com.bakdata.conquery.ConqueryConstants;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;


@UtilityClass
public class FileUtil {
	public static final Pattern SAVE_FILENAME_REPLACEMENT_MATCHER = Pattern.compile("[^a-zA-Z0-9äÄöÖüÜß .\\-]");


    public static String makeSafeFileName(String label, String fileExtension) {
        return SAVE_FILENAME_REPLACEMENT_MATCHER.matcher(label + "." + fileExtension).replaceAll("_");
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

	/**
	 * Adapted from https://howtodoinjava.com/java/java-security/sha-md5-file-checksum-hash/
	 **/
	@SneakyThrows(NoSuchAlgorithmException.class)
	public static String getFileChecksum(File file) throws IOException {
		//Use SHA-1 algorithm
		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		//Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		//Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount;

		//Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		}

		//close the stream; We don't need it now.
		fis.close();

		//Get the hash's bytes
		byte[] bytes = digest.digest();

		//return complete hash
		return new BigInteger(bytes).toString(16);
	}

}
