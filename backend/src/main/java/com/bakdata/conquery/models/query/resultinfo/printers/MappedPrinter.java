package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.index.InternToExternMapper;

public record MappedPrinter(InternToExternMapper mapper) implements Printer {

	@Override
	public String print(Object f) {
		return mapper.external(((String) f));
	}
}
