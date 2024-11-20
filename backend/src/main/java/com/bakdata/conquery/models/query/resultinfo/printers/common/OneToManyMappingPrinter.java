package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Collection;

import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record OneToManyMappingPrinter(InternToExternMapper mapper) implements Printer<String> {
	@Override
	public Collection<String> apply(@NotNull String f) {
		return mapper.externalMultiple(f);
	}
}
