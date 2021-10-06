package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

// hat im Grunde keine Logik drinne und erzeugt nur einen Parser der seine Eingabewerte ignoriert
@Slf4j
@ToString(callSuper = true)
public class CompoundDateRangeParser extends Parser<CDateRange, DateRangeStore> {

	final private String min, max;


	public CompoundDateRangeParser(ConqueryConfig config, String min, String max) {

		super(config);
		this.max = max;
		this.min = min;

	}


	@Override
	protected CDateRange parseValue(@NotNull String value) throws ParsingException {
		return null;
	}

	//ToDo erzeugt ein DateRangeStore aus seinen Geschwistern (auf Buckets Ebene) bezogen

	@Override
	protected DateRangeStore decideType() {

		return null;
	}


	@Override
	public void setValue(DateRangeStore store, int event, CDateRange value) {
	}

	@Override
	public ColumnValues createColumnValues() {
		return new ListColumnValues<>();
	}
}
