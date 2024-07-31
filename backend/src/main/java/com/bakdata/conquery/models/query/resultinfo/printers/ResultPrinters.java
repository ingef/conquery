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
			case INTEGER -> ResultPrinters::printInteger;
			case NUMERIC -> ResultPrinters::printNumeric;
			case DATE -> ResultPrinters::printDate;
			case DATE_RANGE -> ResultPrinters::printDateRange;
			case STRING -> ResultPrinters::printString;
			case MONEY -> ResultPrinters::printMoney;
		};
	}

	public static String printString(PrintSettings printSettings, Object o) {
		return Objects.toString(o);
	}

	public String printInteger(PrintSettings cfg, Object f) {
		if (cfg.isPrettyPrint()) {
			return cfg.getIntegerFormat().format(((Number) f).longValue());
		}

		return f.toString();
	}

	public String printNumeric(PrintSettings cfg, Object f) {
		if (cfg.isPrettyPrint()) {
			return cfg.getDecimalFormat().format(((Number) f).longValue());
		}

		return f.toString();
	}

	public String printMoney(PrintSettings cfg, Object f) {

		if (cfg.isPrettyPrint()) {

			return cfg.getDecimalFormat().format(readMoney(cfg, (Number) f));
		}

		return f.toString();
	}

	public BigDecimal readMoney(PrintSettings cfg, Number value) {
		return new BigDecimal(value.longValue()).movePointLeft(cfg.getCurrency().getDefaultFractionDigits());
	}

	/**
	 * @implNote this is just a convenience method.
	 */
	public String printDateSet(PrintSettings cfg, Object f) {
		return printList(cfg, f, ResultPrinters::printDateRange);
	}

	public String printList(PrintSettings cfg, Object f, Printer elementPrinter) {
		//TODO unify with ListPrinter

		// Jackson deserializes collections as lists instead of an array, if the type is not given
		Preconditions.checkArgument(f instanceof List, "Expected a List got %s (Type: %s, as string: %s)".formatted(f, f.getClass().getName(), f));

		final LocaleConfig.ListFormat listFormat = cfg.getListFormat();
		final StringJoiner joiner = listFormat.createListJoiner();

		for (Object obj : (List<?>) f) {
			joiner.add(listFormat.escapeListElement(elementPrinter.print(cfg, obj)));
		}
		return joiner.toString();
	}

	public static String printDateRange(PrintSettings cfg, Object f) {
		Preconditions.checkArgument(f instanceof List<?>, "Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f);
		Preconditions.checkArgument(((List<?>) f).size() == 2, "Expected a list with 2 elements, one min, one max. The list was: %s ", f);

		final List<?> list = (List<?>) f;
		final Integer min = (Integer) list.get(0);
		final Integer max = (Integer) list.get(1);

		if (min == null || max == null) {
			log.warn("Encountered incomplete range, treating it as an open range. Either min or max was null: {}", list);
		}
		// Compute minString first because we need it either way
		final String minString = min == null || min == CDateRange.NEGATIVE_INFINITY ? "-∞" : printDate(cfg, min);

		if (cfg.isPrettyPrint() && min != null && min.equals(max)) {
			// If the min and max are the same we print it like a singe date, not a range (only in pretty printing)
			return minString;
		}
		final String maxString = max == null || max == CDateRange.POSITIVE_INFINITY ? "+∞" : printDate(cfg, max);

		return minString + cfg.getDateRangeSeparator() + maxString;
	}

	public String printDate(PrintSettings cfg, Object f) {
		if (!(f instanceof Number)) {
			throw new IllegalStateException("Expected an Number but got an '" + f.getClass().getName() + "' with the value: " + f);
		}
		final Number number = (Number) f;
		return cfg.getDateFormatter().format(CDate.toLocalDate(number.intValue()));
	}

	public String printBoolean(PrintSettings cfg, Object f) {
		Preconditions.checkArgument(f instanceof Boolean, "Expected boolean value, but got %s", f.getClass().getName());

		if (cfg.isPrettyPrint()) {
			final Results results = cfg.getLocalized(Results.class);
			return (Boolean) f ? results.True() : results.False();
		}

		return (Boolean) f ? "1" : "0";
	}

	@FunctionalInterface
	public interface Printer {
		String print(PrintSettings cfg, Object f);
	}

	public static class BooleanPrinter implements Printer {
		private String trueVal = null;
		private String falseVal = null;

		@Override
		public String print(PrintSettings cfg, Object f) {
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
		public String print(PrintSettings cfg, Object f) {
			return mapper.external(((String) f));
		}
	}

	public record ListPrinter(Printer printerImpl) implements Printer {

		@Override
		public String print(PrintSettings cfg, Object f) {
			return printList(cfg, f, printerImpl);
		}
	}
}
