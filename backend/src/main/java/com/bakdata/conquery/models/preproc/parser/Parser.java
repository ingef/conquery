package com.bakdata.conquery.models.preproc.parser;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Base class used for parsing values in Preprocessing.
 * <p>
 * Values are fed into value by value through {@link #parseAndAdd(String)} from CSV, internally analyzed and then an appropriate representation fed into the {@link ColumnStore} that was produced using {@link #findBestType()}.
 *
 * @param <MAJOR_JAVA_TYPE> Storage class for preprocessing after parsing.
 * @param <STORE_TYPE>      Root {@link ColumnStore} that can handle the resulting value types of <MAJOR_JAVA_TYPE>.
 */
@Getter
@Setter
@ToString
public abstract class Parser<MAJOR_JAVA_TYPE, STORE_TYPE extends ColumnStore> {

	@ToString.Exclude
	private final ConqueryConfig config;
	@ToString.Exclude
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ColumnValues<MAJOR_JAVA_TYPE> columnValues;

	private int lines = 0;
	private int nullLines = 0;


	protected Parser(ConqueryConfig config, ColumnValues<MAJOR_JAVA_TYPE> columnValues) {
		this.config = config;
		this.columnValues = columnValues;
	}

	public int parseAndAdd(String v) throws ParsingException {
		if (v == null) {
			return add(null);
		}
		try {
			return add(parseValue(v));
		}
		catch (Exception e) {
			throw parsingException(v, e);
		}
	}

	protected final ParsingException parsingException(String value, Exception cause) {
		return new ParsingException("Failed to parse '" + value + "' with " + this.getClass().getSimpleName(), cause);
	}

	/**
	 * Read a raw CSV-value and return a parsed representation.
	 */
	protected abstract MAJOR_JAVA_TYPE parseValue(@Nonnull String value) throws ParsingException;

	public final STORE_TYPE findBestType() {
		return findBestType(getLines());
	}

	/**
	 * Select the optimal store based on all registered values, but allocate it for the supplied number of lines.
	 */
	public final STORE_TYPE findBestType(int storeLines) {
		if (storeLines < 0) {
			throw new IllegalArgumentException("Store lines must not be negative");
		}

		if (isEmpty()) {
			return (STORE_TYPE) EmptyStore.INSTANCE; // This implements all root ColumnStores.
		}

		return decideType(storeLines);
	}

	public boolean isEmpty() {
		return getLines() == 0 || getLines() == getNullLines();
	}

	/**
	 * Analyze all values and select an optimal store.
	 */
	protected STORE_TYPE decideType() {
		return decideType(getLines());
	}

	/**
	 * Select the store representation from the globally collected parser statistics and allocate it with the supplied length.
	 */
	protected abstract STORE_TYPE decideType(int storeLines);

	/**
	 * Process a single parsed line.
	 *
	 * @param v a parsed value.
	 */
	public int add(MAJOR_JAVA_TYPE v) {
		if (v == null) {
			recordNullLine();
		}
		else {
			recordObjectLine(v);
			registerValue(v);
		}
		return requireColumnValues().add(v);
	}

	protected final void recordNullLine() {
		lines++;
		nullLines++;
	}

	protected final void recordObjectLine(Object value) {
		lines++;
	}

	protected final void recordLongLine(long value) {
		lines++;
	}

	protected final void recordDoubleLine(double value) {
		lines++;
	}

	protected final void recordBooleanLine(boolean value) {
		lines++;
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


	public final ColumnValues<MAJOR_JAVA_TYPE> requireColumnValues() {
		if (columnValues == null) {
			throw new IllegalStateException("Column values have already been released");
		}

		return columnValues;
	}

	public final void truncateColumnValues(int size) {
		requireColumnValues().truncate(size);
	}

	public final void releaseColumnValues() {
		columnValues = null;
	}

}
