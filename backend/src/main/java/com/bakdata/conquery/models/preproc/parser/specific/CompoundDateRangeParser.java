package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeCompound;
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

	final private String startColumn, endColumn;


	public CompoundDateRangeParser(ConqueryConfig config, String startColumn, String endColumn) {
		super(config);
		this.endColumn = endColumn;
		this.startColumn = startColumn;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	protected CDateRange parseValue(@NotNull String value) throws ParsingException {
		return null;
	}

	@Override
	protected DateRangeStore decideType() {
		return new DateRangeTypeCompound(this.startColumn, this.endColumn);
	}


	@Override
	public void setValue(DateRangeStore store, int event, CDateRange value) {
	}

	@Override
	public ColumnValues createColumnValues() {
		return null;
	}
}
