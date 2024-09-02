package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record MappedPrinter(InternToExternMapper mapper) implements Printer {

	@Override
	public String apply(Object f) {
		return mapper.external(((String) f));
	}
}
