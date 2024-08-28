package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvResultPrinters extends PrinterFactory {


	@Override
	public Printer getListPrinter(Printer elementPrinter, PrintSettings printSettings) {
		return new ListPrinter(elementPrinter, printSettings);
	}

	@Override
	public Printer getBooleanPrinter(PrintSettings printSettings) {
		return new BooleanPrinter(printSettings);
	}

	@Override
	public Printer getIntegerPrinter(PrintSettings printSettings) {
		return new IntegerPrinter(printSettings);
	}

	@Override
	public Printer getNumericPrinter(PrintSettings printSettings) {
		return new NumericPrinter(printSettings);
	}

	@Override
	public Printer getDatePrinter(PrintSettings printSettings) {
		return new DatePrinter(printSettings);
	}

	@Override
	public Printer getDateRangePrinter(PrintSettings printSettings) {
		return new DateRangePrinter(printSettings);
	}

	@Override
	public Printer getStringPrinter(PrintSettings printSettings) {
		return new StringPrinter();
	}

	@Override
	public Printer getMoneyPrinter(PrintSettings printSettings) {
		return new MoneyPrinter(printSettings);
	}

	private record StringPrinter() implements Printer {
		@Override
		public String print(Object f) {
			return Objects.toString(f);
		}
	}

	private record IntegerPrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getIntegerFormat().format(((Number) f).longValue());
			}

			return f.toString();
		}
	}

	private record NumericPrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(f);
			}

			return f.toString();
		}
	}

	private record MoneyPrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {

			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(f);
			}

			return f.toString();
		}
	}

	private record DatePrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {
			Preconditions.checkArgument(f instanceof Number, "Expected an Number but got an '%s' with the value: %s".formatted(f.getClass().getName(), f));

			final Number number = (Number) f;
			return cfg.getDateFormatter().format(CDate.toLocalDate(number.intValue()));
		}
	}

	private record DateRangePrinter(DatePrinter datePrinter, PrintSettings cfg) implements Printer {

		public DateRangePrinter(PrintSettings printSettings) {
			this(new DatePrinter(printSettings), printSettings);
		}

		@Override
		public String print(Object f) {
			Preconditions.checkArgument(f instanceof List<?>, "Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f);
			Preconditions.checkArgument(((List<?>) f).size() == 2, "Expected a list with 2 elements, one min, one max. The list was: %s ", f);

			final List<?> list = (List<?>) f;
			final Integer min = (Integer) list.get(0);
			final Integer max = (Integer) list.get(1);

			if (min == null || max == null) {
				log.warn("Encountered incomplete range, treating it as an open range. Either min or max was null: {}", list);
			}
			// Compute minString first because we need it either way
			final String minString = min == null || min == CDateRange.NEGATIVE_INFINITY ? "-∞" : datePrinter.print(min);

			if (cfg.isPrettyPrint() && min != null && min.equals(max)) {
				// If the min and max are the same we print it like a singe date, not a range (only in pretty printing)
				return minString;
			}
			final String maxString = max == null || max == CDateRange.POSITIVE_INFINITY ? "+∞" : datePrinter.print(max);

			return minString + cfg.getDateRangeSeparator() + maxString;
		}
	}

	private record BooleanPrinter(PrintSettings cfg, String trueVal, String falseVal) implements Printer {

		public BooleanPrinter(PrintSettings cfg) {
			this(cfg, cfg.isPrettyPrint() ? C10nCache.getLocalized(Results.class, cfg.getLocale()).True() : "1", cfg.isPrettyPrint()
																												 ? C10nCache.getLocalized(Results.class, cfg.getLocale()).False()
																												 : "0");
		}

		@Override
		public String print(Object f) {
			if ((Boolean) f) {
				return trueVal;
			}
			return falseVal;

		}
	}

	private record ListPrinter(Printer elementPrinter, PrintSettings cfg, LocaleConfig.ListFormat listFormat) implements Printer {

		public ListPrinter(Printer elementPrinter, PrintSettings cfg) {
			this(elementPrinter, cfg, cfg.getListFormat());
		}

		@Override
		public String print(Object f) {

			// Jackson deserializes collections as lists instead of an array, if the type is not given
			Preconditions.checkArgument(f instanceof List, "Expected a List got %s (as String `%s` )".formatted(f.getClass().getName(), f));

			final StringJoiner joiner = listFormat.createListJoiner();

			for (Object obj : (List<?>) f) {
				joiner.add(listFormat.escapeListElement(elementPrinter.print(obj).toString()));
			}
			return joiner.toString();
		}
	}

}
