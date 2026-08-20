package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record OneToOneMappingPrinter(InternToExternMapper mapper, Printer<String> andThen) implements Printer<String> {

	@Override
	public Object apply(@NotNull String f) {
		String external = mapper.external(f);
		if (external == null){
			return f;
		}
		return andThen.apply(external);
	}
}
