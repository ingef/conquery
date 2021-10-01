package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

// hat im Grunde keine Logik drinne und erzeugt nur einen Parser der seine Eingabewerte ignoriert
@Slf4j
@ToString(callSuper = true)
public class CompoundDateRangeParser extends Parser<CDateRange, DateRangeStore> {


	//ToDo 2 string types min max


	public CompoundDateRangeParser(ConqueryConfig config) {
		super(config);
	}

	/**
	 * Read a raw CSV-value and return a parsed representation.
	 *
	 * @param value
	 */
	@Override
	protected CDateRange parseValue(@NotNull String value) throws ParsingException {
		return null;
	}

	/**
	 * Analyze all values and select an optimal store.
	 * //ToDo erzeugt ein DateRangeStore aus seinen Geschwistern (auf Buckets Ebene) bezogen
	 */
	@Override
	protected DateRangeStore decideType() {
		return null;
	}

	/**
	 * Write a parsed value into the store. This allows type-safe generic {@link ColumnStore} implementations.
	 *
	 * @param store
	 * @param event
	 * @param value
	 */
	@Override
	public void setValue(DateRangeStore store, int event, CDateRange value) {

	}

	@Override
	public ColumnValues createColumnValues() {
		return null;
	}
}
