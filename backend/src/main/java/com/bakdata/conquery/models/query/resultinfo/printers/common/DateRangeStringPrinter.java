package com.bakdata.conquery.models.query.resultinfo.printers.common;

import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record DateRangeStringPrinter(DateStringPrinter datePrinter, PrintSettings cfg) implements Printer {

	public DateRangeStringPrinter(PrintSettings printSettings) {
		this(new DateStringPrinter(printSettings), printSettings);
	}

	@Override
	public String apply(Object f) {
		Preconditions.checkArgument(f instanceof List<?>, "Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f);
		Preconditions.checkArgument(((List<?>) f).size() == 2, "Expected a list with 2 elements, one min, one max. The list was: %s ", f);

		final List<?> list = (List<?>) f;
		final Integer min = (Integer) list.get(0);
		final Integer max = (Integer) list.get(1);

		if (min == null || max == null) {
			log.warn("Encountered incomplete range, treating it as an open range. Either min or max was null: {}", list);
		}
		// Compute minString first because we need it either way
		final String minString = min == null || min == CDateRange.NEGATIVE_INFINITY ? "-∞" : datePrinter.apply(min);

		if (cfg.isPrettyPrint() && min != null && min.equals(max)) {
			// If the min and max are the same we print it like a singe date, not a range (only in pretty printing)
			return minString;
		}
		final String maxString = max == null || max == CDateRange.POSITIVE_INFINITY ? "+∞" : datePrinter.apply(max);

		return minString + cfg.getDateRangeSeparator() + maxString;
	}
}
