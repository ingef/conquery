package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record OneToOneMappingPrinter(InternToExternMapper mapper) implements Printer<String> {

	@Override
	public String apply(@NotNull String f) {
		return mapper.external(f);
	}
}
