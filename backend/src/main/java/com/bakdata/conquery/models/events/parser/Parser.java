package com.bakdata.conquery.models.events.parser;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.BitSet;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class used for parsing values in Preprocessing.
 * <p>
 * Values are fed into value by value into {@link #parse(String)} from CSV, internally analyzed and then an appropriate representation fed into the {@link ColumnStore} that was produced using {@link #findBestType()}.
 *
 * @param <MAJOR_JAVA_TYPE> Storage class for preprocessing after parsing.
 * @param <STORE_TYPE>      Root {@link ColumnStore} that can handle the resulting value types of <MAJOR_JAVA_TYPE>.
 */
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@Slf4j
public abstract class Parser<MAJOR_JAVA_TYPE, STORE_TYPE extends ColumnStore> {

	private final ParserConfig config;

	private int lines = 0;
	private int nullLines = 0;


	public final MAJOR_JAVA_TYPE parse(String v) throws ParsingException {
		if (v == null) {
			return null;
		}
		try {
			return parseValue(v);
		}
		catch (Exception e) {
			throw new ParsingException("Failed to parse '" + v + "' with " + this.getClass().getSimpleName(), e);
		}
	}

	/**
	 * Read a raw CSV-value and return a parsed representation.
	 */
	protected abstract MAJOR_JAVA_TYPE parseValue(@Nonnull String value) throws ParsingException;

	public final STORE_TYPE findBestType() {
		if (getLines() == 0 || getLines() == getNullLines()) {
			return (STORE_TYPE) EmptyStore.INSTANCE; // This implements all root ColumnStores.
		}

		return decideType();
	}

	/**
	 * Analyze all values and select an optimal store.
	 */
	protected abstract STORE_TYPE decideType();

	/**
	 * Process a single parsed line.
	 *
	 * @param v a parsed value.
	 */
	public MAJOR_JAVA_TYPE addLine(MAJOR_JAVA_TYPE v) {
		lines++;
		log.trace("Registering `{}` in line {}", v, lines);

		if (v == null) {
			nullLines++;
		}
		else {
			registerValue(v);
		}
		return v;
	}

	/**
	 * Register/Analyze an incoming value for {@link #decideType()}.
	 */
	protected void registerValue(MAJOR_JAVA_TYPE v) {
	}

	/**
	 * Write a parsed value into the store. This allows type-safe generic {@link ColumnStore} implementations.
	 */
	public abstract void setValue(STORE_TYPE store, int event, MAJOR_JAVA_TYPE value);


	public abstract ColumnValues createColumnValues();

	/**
	 * per Column Store to encode null in auxiliary bitset, allowing primitive storage.
	 */
	@SuppressWarnings("Unchecked")
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	public static abstract class ColumnValues<T> {

		private final T nullValue;
		private final BitSet nulls = new BitSet();
		private int size = 0;

		@SneakyThrows
		public static MappedByteBuffer allocateBuffer() {
			final File file = Files.createTempFile("columnvalues", "conquery").toFile();
			file.deleteOnExit();

			final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

			final MappedByteBuffer byteBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 5000000);
			byteBuffer.mark();
			byteBuffer.reset();

			return byteBuffer;
		}

		public boolean isNull(int event) {
			return nulls.get(event);
		}

		public abstract T get(int event);

		public int add(T value) {
			int event = size++;

			if (value == null) {
				nulls.set(event);
				write(event, nullValue);
			}
			else {
				write(event, value);
			}

			return event;
		}

		protected abstract void write(int position, T obj);
	}
}
