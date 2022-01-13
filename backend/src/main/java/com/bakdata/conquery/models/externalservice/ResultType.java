package com.bakdata.conquery.models.externalservice;

import c10n.C10N;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@Slf4j
public abstract class ResultType {

	public String printNullable(PrintSettings cfg, Object f) {
		if (f == null) {
			return "";
		}
		return print(cfg, f);
	}

	protected String print(PrintSettings cfg, @NonNull Object f) {
		return f.toString();
	}

	public abstract String typeInfo();

	public static ResultType resolveResultType(MajorTypeId majorTypeId) {
		switch (majorTypeId) {
			case STRING:
				return StringT.INSTANCE;
			case BOOLEAN:
				return BooleanT.INSTANCE;
			case DATE:
				return DateT.INSTANCE;
			case DATE_RANGE:
				return DateRangeT.INSTANCE;
			case INTEGER:
				return IntegerT.INSTANCE;
			case MONEY:
				return MoneyT.INSTANCE;
			case DECIMAL:
			case REAL:
				return NumericT.INSTANCE;
			default:
				throw new IllegalStateException(String.format("Invalid column type '%s'", majorTypeId));
		}
	}

	abstract static class PrimitiveResultType extends ResultType {
		@Override
		public String typeInfo() {
			return this.getClass().getAnnotation(CPSType.class).id();
		}

		@Override
		public String toString() {
			return typeInfo();
		}
	}

	@CPSType(id = "BOOLEAN", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class BooleanT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final BooleanT INSTANCE = new BooleanT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			Preconditions.checkArgument(f instanceof Boolean, "Expected boolean value, but got %s", f.getClass().getName());

			if(cfg.isPrettyPrint()) {
				//TODO this might be incredibly slow, probably better to cache this in the instance but we need to not use Singletons for that
				return (Boolean) f ? C10N.get(Results.class, cfg.getLocale()).True() : C10N.get(Results.class, cfg.getLocale()).False();
			}

			return (Boolean) f ? "1" : "0";
		}
	}


	@CPSType(id = "INTEGER", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class IntegerT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final IntegerT INSTANCE = new IntegerT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getIntegerFormat().format(((Number) f).longValue());
			}
			return f.toString();
		}
	}

	@CPSType(id = "NUMERIC", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class NumericT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final NumericT INSTANCE = new NumericT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(f);
			}
			return f.toString();
		}
	}

	@CPSType(id = "CATEGORICAL", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class CategoricalT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final CategoricalT INSTANCE = new CategoricalT();
	}

	@CPSType(id = "RESOLUTION", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ResolutionT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final ResolutionT INSTANCE = new ResolutionT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (f instanceof Resolution) {
				return ((Resolution) f).toString(cfg.getLocale());
			}
			try {
				// If the object was parsed as a simple string, try to convert it to a
				// DateContextMode to get Internationalization
				return Resolution.valueOf(f.toString()).toString(cfg.getLocale());
			} catch (Exception e) {
				throw new IllegalArgumentException(f + " is not a valid resolution.", e);
			}
		}
	}

	//TODO introduce semantic type to combine enum based types
	@CPSType(id = "OBSERVATION_SCOPE", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class ObservationScopeT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final ObservationScopeT INSTANCE = new ObservationScopeT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (f instanceof FeatureGroup) {
				return ((FeatureGroup) f).toString(cfg.getLocale());
			}
			try {
				// If the object was parsed as a simple string, try to convert it to a
				// DateContextMode to get Internationalization
				return FeatureGroup.valueOf(f.toString()).toString(cfg.getLocale());
			} catch (Exception e) {
				throw new IllegalArgumentException(f + " is not a valid observation scope.", e);
			}
		}
	}

	@CPSType(id = "DATE", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class DateT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final DateT INSTANCE = new DateT();

		@Override
		public String print(PrintSettings cfg, @NonNull Object f) {
			if(!(f instanceof Number)) {
				throw new IllegalStateException("Expected an Number but got an '" + f.getClass().getName() + "' with the value: " + f);
			}
			final Number number = (Number) f;
			return print(number, cfg.getDateFormatter());
		}

		public static String print(Number num, DateTimeFormatter formatter) {
			return formatter.format(LocalDate.ofEpochDay(num.intValue()));
		}


	}

	/**
	 * A DateRange is provided by in a query result as two ints in a list, both standing for an epoch day (see {@link LocalDate#toEpochDay()}).
	 * The first int describes the included lower bound of the range. The second int descibes the included upper bound.
	 */
	@CPSType(id = "DATE_RANGE", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class DateRangeT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final DateRangeT INSTANCE = new DateRangeT();

		@Override
		public String print(PrintSettings cfg, @NonNull Object f) {
			if(!(f instanceof List)) {
				throw new IllegalStateException(String.format("Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f));
			}
			List<?> list = (List<?>) f;
			if(list.size() != 2) {
				throw new IllegalStateException("Expected a list with 2 elements, one min, one max. The list was: " + list);
			}
			final DateTimeFormatter dateFormat = cfg.getDateFormatter();
			final Integer min = (Integer) list.get(0);
			final Integer max = (Integer) list.get(1);
			if (min == null || max == null) {
				log.warn("Encountered incomplete range, treating it as an open range. Either min or max was null: {}", list);
			}
			// Compute minString first because we need it either way
			String minString = min == null || min == Integer.MIN_VALUE ? "-∞" : ResultType.DateT.print(min, dateFormat);

			if (cfg.isPrettyPrint() && min != null && min.equals(max)){
				// If the min and max are the same we print it like a singe date, not a range (only in pretty printing)
				return minString;
			}
			String maxString = max == null || max == Integer.MAX_VALUE ? "+∞" : ResultType.DateT.print(max, dateFormat);

			return minString + cfg.getDateRangeSeparator() + maxString;
		}
	}

	@CPSType(id = "STRING", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class StringT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final StringT INSTANCE = new StringT();
	}

	@CPSType(id = "ID", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class IdT extends PrimitiveResultType {
		@Getter(onMethod_ = @JsonCreator)
		public static final IdT INSTANCE = new IdT();
	}

	@CPSType(id = "MONEY", base = ResultType.class)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class MoneyT extends PrimitiveResultType {

		@Getter(onMethod_ = @JsonCreator)
		public static final MoneyT INSTANCE = new MoneyT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(new BigDecimal(((Number) f).longValue()).movePointLeft(cfg.getCurrency().getDefaultFractionDigits()));
			}
			return IntegerT.INSTANCE.print(cfg, f);
		}
	}

	@CPSType(id = "LIST", base = ResultType.class)
	@Getter
	public static class ListT extends ResultType {
		@NonNull
		private final ResultType elementType;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public ListT(@NonNull ResultType elementType) {
			this.elementType = elementType;
		}

		@Override
		public String print(PrintSettings cfg, @NonNull Object f) {
			// Jackson deserializes collections as lists instead of an array, if the type is not given
			if(!(f instanceof List)) {
				throw new IllegalStateException(String.format("Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f));
			}
			// Not sure if this escaping is enough
			String listDelimEscape = cfg.getListElementEscaper() + cfg.getListFormat().getSeparator();
			StringJoiner joiner = new StringJoiner(cfg.getListFormat().getSeparator(), cfg.getListFormat().getStart(),cfg.getListFormat().getEnd());
			for(Object obj : (List<?>) f) {
				joiner.add(elementType.print(cfg,obj).replace(cfg.getListFormat().getSeparator(), listDelimEscape));
			}
			return joiner.toString();
		}

		@Override
		public String typeInfo() {
			return this.getClass().getAnnotation(CPSType.class).id() + "[" + elementType.typeInfo() + "]";
		}

		@Override
		public String toString() {
			return typeInfo();
		}
	}
}
