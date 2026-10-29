package com.bakdata.conquery.models.preproc.parser;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Base class used for parsing values in Preprocessing.
 * <p>
 * Values are fed into value by value from CSV, internally analyzed and then an appropriate representation fed into the {@link ColumnStore} that was produced using {@link #createSpecializedStore(int)}.
 *
 * @param <MAJOR_JAVA_TYPE> Storage class for preprocessing after parsing.
 * @param <STORE_TYPE>      Root {@link ColumnStore} that can handle the resulting value types of <MAJOR_JAVA_TYPE>.
 * @param <COLUMN_VALUES_TYPE> Staging buffer for values of <MAJOR_JAVA_TYPE>.
 */
@Getter
@Setter
@ToString
public abstract class Parser<MAJOR_JAVA_TYPE, STORE_TYPE extends ColumnStore, COLUMN_VALUES_TYPE extends ColumnValues<MAJOR_JAVA_TYPE>> {

	@ToString.Exclude
	private final ConqueryConfig config;
	@ToString.Exclude
	@Setter(AccessLevel.NONE)
	private COLUMN_VALUES_TYPE columnValues;

	private int lines = 0;
	private int nullLines = 0;


	protected Parser(ConqueryConfig config, COLUMN_VALUES_TYPE columnValues) {
		this.config = config;
		this.columnValues = columnValues;
	}

	public void addNull() {
		recordNullLine();
		getColumnValues().addNull();
	}

    /**
	 * Select the optimal store based on all registered values, but allocate it for the supplied number of lines.
	 */
	public final STORE_TYPE createSpecializedStore(int storeLines) {
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
	 * Select the store representation from the globally collected parser statistics and allocate it with the supplied length.
	 */
	protected abstract STORE_TYPE decideType(int storeLines);

	public final void recordNullLine() {
		recordLine();
		nullLines++;
	}

	public final void recordLine() {
		lines++;
	}

	/**
	 * Write a parsed value into the store. This allows type-safe generic {@link ColumnStore} implementations.
	 */
	public abstract void setValue(STORE_TYPE store, int event, MAJOR_JAVA_TYPE value);

	public final void truncateColumnValues(int size) {
		getColumnValues().truncate(size);
	}

	public final void releaseColumnValues() {
		columnValues = null;
	}

}
