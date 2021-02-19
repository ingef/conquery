package com.bakdata.conquery.models.events.parser.specific;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

import com.bakdata.conquery.models.events.parser.ColumnValues;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BufferBackedColumnValues<T> extends ColumnValues<T> {

	@Getter
	private final MappedByteBuffer buffer;
	@Getter
	private final File bufferFile;
	private final RandomAccessFile randomAccessFile;

	@SneakyThrows
	public BufferBackedColumnValues(T nullValue, int bufferSizeBytes) {
		super(nullValue);

		bufferFile = Files.createTempFile("columnvalues", "conquery").toFile();
		bufferFile.deleteOnExit();


		randomAccessFile = new RandomAccessFile(bufferFile, "rw");
		buffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, bufferSizeBytes);
	}

	@SneakyThrows
	@Override
	public void close() {
		buffer.clear();

		randomAccessFile.getChannel().close();
		randomAccessFile.close();


		Files.delete(bufferFile.toPath());
	}
}
