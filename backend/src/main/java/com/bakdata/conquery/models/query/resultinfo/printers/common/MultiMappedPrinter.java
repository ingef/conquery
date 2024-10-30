package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record MultiMappedPrinter(InternToExternMapper mapping) implements Printer<Collection<String>> {
	@Override
	public Object apply(@NotNull Collection<String> value) {
		Set<String> out = new HashSet<>();

		for (String elt : value) {
			out.addAll(mapping.externalMultiple(elt));
		}

		return out;
	}
}
