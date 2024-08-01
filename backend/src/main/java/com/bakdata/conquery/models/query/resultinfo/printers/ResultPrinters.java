package com.bakdata.conquery.models.query.resultinfo.printers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ResultPrinters {

	public Printer defaultPrinter(ResultType type) {
		if (type instanceof ResultType.ListT<?> listT) {
			return new ListPrinter(defaultPrinter(listT.getElementType()));
		}

		return switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> new BooleanPrinter();
			case INTEGER -> new IntegerPrinter();
			case NUMERIC -> new NumericPrinter();
			case DATE -> new DatePrinter();
			case DATE_RANGE -> new DateRangePrinter();
			case STRING -> new StringPrinter();
			case MONEY -> new MoneyPrinter();
		};
	}

	public BigDecimal readMoney(PrintSettings cfg, Number value) {
		return new BigDecimal(value.longValue()).movePointLeft(cfg.getCurrency().getDefaultFractionDigits());
	}

	public String printDate(PrintSettings cfg, Object f) {
		if (!(f instanceof Number)) {
			throw new IllegalStateException("Expected an Number but got an '" + f.getClass().getName() + "' with the value: " + f);
		}
		final Number number = (Number) f;
		return cfg.getDateFormatter().format(CDate.toLocalDate(number.intValue()));
	}

	@FunctionalInterface
	public interface Printer {
		String print(Object f, PrintSettings cfg);
	}

	public static class StringPrinter implements Printer {
		@Override
		public String print(Object f, PrintSettings cfg) {
			return Objects.toString(f);
		}
	}

	public static class IntegerPrinter implements Printer {
		@Override
		public String print(Object f, PrintSettings cfg) {
			if (cfg.isPrettyPrint()) {
				return cfg.getIntegerFormat().format(((Number) f).longValue());
			}

			return f.toString();
		}
	}

	public static class NumericPrinter implements Printer {

		@Override
		public String print(Object f, PrintSettings cfg) {
			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(((Number) f).longValue());
			}

			return f.toString();
		}
	}

	public static class MoneyPrinter implements Printer {

		@Override
		public String print(Object f, PrintSettings cfg) {

			if (cfg.isPrettyPrint()) {

				return cfg.getDecimalFormat().format(readMoney(cfg, (Number) f));
			}

			return f.toString();
		}
	}

	public static class DatePrinter implements Printer {
		@Override
		public String print(Object f, PrintSettings cfg) {
			Preconditions.checkArgument(f instanceof Number, "Expected an Number but got an '%s' with the value: %s".formatted(f.getClass().getName(), f));

			final Number number = (Number) f;
			return cfg.getDateFormatter().format(CDate.toLocalDate(number.intValue()));
		}
	}

	public static class DateRangePrinter implements Printer {

		private final DatePrinter datePrinter = new DatePrinter();

		@Override
		public String print(Object f, PrintSettings cfg) {
			Preconditions.checkArgument(f instanceof List<?>, "Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f);
			Preconditions.checkArgument(((List<?>) f).size() == 2, "Expected a list with 2 elements, one min, one max. The list was: %s ", f);

			final List<?> list = (List<?>) f;
			final Integer min = (Integer) list.get(0);
			final Integer max = (Integer) list.get(1);

			if (min == null || max == null) {
				log.warn("Encountered incomplete range, treating it as an open range. Either min or max was null: {}", list);
			}
			// Compute minString first because we need it either way
			final String minString = min == null || min == CDateRange.NEGATIVE_INFINITY ? "-∞" : datePrinter.print(min, cfg);

			if (cfg.isPrettyPrint() && min != null && min.equals(max)) {
				// If the min and max are the same we print it like a singe date, not a range (only in pretty printing)
				return minString;
			}
			final String maxString = max == null || max == CDateRange.POSITIVE_INFINITY ? "+∞" : datePrinter.print(max, cfg);

			return minString + cfg.getDateRangeSeparator() + maxString;
		}
	}

	public static class BooleanPrinter implements Printer {
		private String trueVal = null;
		private String falseVal = null;

		@Override
		public String print(Object f, PrintSettings cfg) {
			Preconditions.checkArgument(f instanceof Boolean, "Expected boolean value, but got %s", f.getClass().getName());

			if (!cfg.isPrettyPrint()) {
				return (Boolean) f ? "1" : "0";
			}

			if (trueVal == null) {
				trueVal = cfg.getLocalized(Results.class).True();
				falseVal = cfg.getLocalized(Results.class).False();
			}

			return (Boolean) f ? trueVal : falseVal;

		}
	}

	public record MappedPrinter(InternToExternMapper mapper) implements Printer {

		@Override
		public String print(Object f, PrintSettings cfg) {
			return mapper.external(((String) f));
		}
	}

	public record ListPrinter(Printer printerImpl) implements Printer {

		@Override
		public String print(Object f, PrintSettings cfg) {

			// Jackson deserializes collections as lists instead of an array, if the type is not given
			Preconditions.checkArgument(f instanceof List, "Expected a List got %s (Type: %s, as string: %s)".formatted(f, f.getClass().getName(), f));

			final LocaleConfig.ListFormat listFormat = cfg.getListFormat();
			final StringJoiner joiner = listFormat.createListJoiner();

			for (Object obj : (List<?>) f) {
				joiner.add(listFormat.escapeListElement(printerImpl.print(obj, cfg)));
			}
			return joiner.toString();
		}
	}
}
