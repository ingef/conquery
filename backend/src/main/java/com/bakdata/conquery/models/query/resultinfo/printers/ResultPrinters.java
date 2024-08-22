package com.bakdata.conquery.models.query.resultinfo.printers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.LocalizedToString;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ResultPrinters {

	public Printer printerFor(ResultType type, PrintSettings printSettings) {
		if (type instanceof ResultType.ListT<?> listT) {
			return new ListPrinter(printerFor(listT.getElementType(), printSettings), printSettings);
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

	public record StringPrinter() implements Printer {
		@Override
		public String print(Object f) {
			return Objects.toString(f);
		}
	}

	public record IntegerPrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getIntegerFormat().format(((Number) f).longValue());
			}

			return f.toString();
		}
	}

	public record NumericPrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(f);
			}

			return f.toString();
		}
	}

	public record MoneyPrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {

			if (cfg.isPrettyPrint()) {

				return cfg.getDecimalFormat().format(readMoney(cfg, (Number) f));
			}

			return f.toString();
		}
	}

	public record DatePrinter(PrintSettings cfg) implements Printer {

		@Override
		public String print(Object f) {
			Preconditions.checkArgument(f instanceof Number, "Expected an Number but got an '%s' with the value: %s".formatted(f.getClass().getName(), f));

			final Number number = (Number) f;
			return cfg.getDateFormatter().format(CDate.toLocalDate(number.intValue()));
		}
	}

	public record DateRangePrinter(DatePrinter datePrinter, PrintSettings cfg) implements Printer {

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

	public record BooleanPrinter(PrintSettings cfg, String trueVal, String falseVal) implements Printer {

		public BooleanPrinter(PrintSettings cfg) {
			this(
					cfg,
					cfg.isPrettyPrint() ? C10nCache.getLocalized(Results.class, cfg.getLocale()).True() : "1",
					cfg.isPrettyPrint() ? C10nCache.getLocalized(Results.class, cfg.getLocale()).False() : "0"
			);
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
		public String print(Object rawValue) {
			if (rawValue == null) {
				return null;
			}

			final int localId = (int) rawValue;

			final ConceptTreeNode<?> node = ((TreeConcept) concept).getElementByLocalId(localId);

			if (!cfg.isPrettyPrint()) {
				return node.getId().toString();
			}

			if (node.getDescription() == null) {
				return node.getLabel();
			}

			return node.getLabel() + " - " + node.getDescription();
		}
	}

	public record ListPrinter(Printer elementPrinter, PrintSettings cfg, LocaleConfig.ListFormat listFormat) implements Printer {

		public ListPrinter(Printer elementPrinter, PrintSettings cfg) {
			this(elementPrinter, cfg, cfg.getListFormat());
		}

		@Override
		public String print(Object f) {

			// Jackson deserializes collections as lists instead of an array, if the type is not given
			Preconditions.checkArgument(f instanceof List, "Expected a List got %s (as String `%s` )".formatted(f.getClass().getName(), f));

			final StringJoiner joiner = listFormat.createListJoiner();

			for (Object obj : (List<?>) f) {
				joiner.add(listFormat.escapeListElement(elementPrinter.print(obj)));
			}
			return joiner.toString();
		}
	}

	public record LocalizedEnumPrinter<T extends Enum<T> & LocalizedToString>(PrintSettings cfg, Class<T> clazz) implements Printer {
		@Override
		public String print(Object f) {

			if (clazz.isInstance(f)) {
				return clazz.cast(f).toString(cfg.getLocale());
			}
			try {
				return Enum.valueOf(clazz, f.toString()).toString(cfg.getLocale());
			}
			catch (Exception e) {
				throw new IllegalArgumentException("%s is not a valid %s.".formatted(f, clazz), e);
			}
		}
	}
}
