package com.bakdata.conquery.util.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;


@UtilityClass
public class FileUtil {
    public static final Pattern SAVE_FILENAME_REPLACEMENT_MATCHER = Pattern.compile("[^a-zA-Z0-9\\.\\-]");

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

}
