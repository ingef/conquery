package com.bakdata.conquery.util.io;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

/**
 * Allows URIs (URLs) to start with the schema "classpath" to reference files on the classpath through a configuration.
 * <p>
 * This is useful for testing components such as the {@link com.bakdata.eva.plugins.PostalCodePlugin}.
 * In the test environment it uses a small dataset, that is on the classpath.
 * Then in production a "file://" scheme might be uses to reference a large dataset.
 */
@AutoService(FileSystemProvider.class)
public class ClassPathFileSystemProvider extends FileSystemProvider {
	@Override
	public String getScheme() {
		return "classpath";
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		throw new UnsupportedOperationException();
	}

	@NotNull
	@Override
	@SneakyThrows({FileNotFoundException.class, URISyntaxException.class})
	public Path getPath(@NotNull URI uri) {
		final URL resource = this.getClass().getResource(uri.getPath());
		if (resource == null) {
			throw new FileNotFoundException(uri.toString());
		}
		return Path.of(resource.toURI());
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(Path path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSameFile(Path path, Path path2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isHidden(Path path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FileStore getFileStore(Path path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
		throw new UnsupportedOperationException();
	}
}
