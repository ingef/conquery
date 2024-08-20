package com.bakdata.conquery.models.query.resultinfo.printers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ResultPrinters {

	public Printer defaultPrinter(ResultType type, PrintSettings printSettings) {
		if (type instanceof ResultType.ListT<?> listT) {
			return new ListPrinter(defaultPrinter(listT.getElementType(), printSettings), printSettings);
		}

		return switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> new BooleanPrinter(printSettings);
			case INTEGER -> new IntegerPrinter(printSettings);
			case NUMERIC -> new NumericPrinter(printSettings);
			case DATE -> new DatePrinter(printSettings);
			case DATE_RANGE -> new DateRangePrinter(printSettings);
			case STRING -> new StringPrinter();
			case MONEY -> new MoneyPrinter(printSettings);
		};
	}

	public BigDecimal readMoney(PrintSettings cfg, Number value) {
		return new BigDecimal(value.longValue()).movePointLeft(cfg.getCurrency().getDefaultFractionDigits());
	}

	public interface Printer {
		String print(Object f);
	}

	@ToString()
	public static class StringPrinter implements Printer {
		@Override
		public String print(Object f) {
			return Objects.toString(f);
		}
	}


	@ToString()
	@RequiredArgsConstructor
	public static class IntegerPrinter implements Printer {
		private final PrintSettings cfg;

		@Override
		public String print(Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getIntegerFormat().format(((Number) f).longValue());
			}

			return f.toString();
		}
	}

	@ToString()
	@RequiredArgsConstructor
	public static class NumericPrinter implements Printer {
		private final PrintSettings cfg;

		@Override
		public String print(Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(f);
			}

			return f.toString();
		}
	}

	@ToString()
	@RequiredArgsConstructor
	public static class MoneyPrinter implements Printer {
		private final PrintSettings cfg;

		@Override
		public String print(Object f) {

			if (cfg.isPrettyPrint()) {

				return cfg.getDecimalFormat().format(readMoney(cfg, (Number) f));
			}

			return f.toString();
		}
	}

	@ToString()
	@RequiredArgsConstructor
	public static class DatePrinter implements Printer {
		private final PrintSettings cfg;

		@Override
		public String print(Object f) {
			Preconditions.checkArgument(f instanceof Number, "Expected an Number but got an '%s' with the value: %s".formatted(f.getClass().getName(), f));

			final Number number = (Number) f;
			return cfg.getDateFormatter().format(CDate.toLocalDate(number.intValue()));
		}
	}

	@ToString()

	public static class DateRangePrinter implements Printer {
		private final DatePrinter datePrinter;
		private final PrintSettings cfg;

		public DateRangePrinter(PrintSettings printSettings){
			datePrinter = new DatePrinter(printSettings);
			cfg = printSettings;
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

	@ToString()
	public static class BooleanPrinter implements Printer {
		private final PrintSettings cfg;

		private final String trueVal;
		private final String falseVal;

		public BooleanPrinter(PrintSettings cfg) {
			this.cfg = cfg;
			if (!cfg.isPrettyPrint()) {
				trueVal =  "1";
				falseVal = "0";
			}
			else {
				trueVal = C10nCache.getLocalized(Results.class, cfg.getLocale()).True();
				falseVal = C10nCache.getLocalized(Results.class, cfg.getLocale()).False();
			}
		}

		@Override
		public String print(Object f) {
			if ((Boolean) f) {
				return trueVal;
			}
			return falseVal;

		}
	}

	public record MappedPrinter(InternToExternMapper mapper) implements Printer {

		@Override
		public String print(Object f) {
			return mapper.external(((String) f));
		}
	}


	public record ConceptIdPrinter(Concept concept, PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {
			return concept.printConceptLocalId(cfg, f);
		}
	}

	public record ListPrinter(Printer elementPrinter, PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {

			// Jackson deserializes collections as lists instead of an array, if the type is not given
			Preconditions.checkArgument(f instanceof List, "Expected a List got %s (as String `%s` )".formatted(f.getClass().getName(), f));

			final LocaleConfig.ListFormat listFormat = cfg.getListFormat();
			final StringJoiner joiner = listFormat.createListJoiner();

			for (Object obj : (List<?>) f) {
				joiner.add(listFormat.escapeListElement(elementPrinter.print(obj)));
			}
			return joiner.toString();
		}
	}
}
