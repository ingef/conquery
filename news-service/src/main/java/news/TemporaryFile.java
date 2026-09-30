package news;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public record TemporaryFile(Path path) implements AutoCloseable {

    public static TemporaryFile create(Path directory) throws IOException {
        return new TemporaryFile(Files.createTempFile(directory, ".tmp-", ""));
    }

    public void moveTo(Path target) throws IOException {
        try {
            Files.move(path, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(path, target);
        }
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
