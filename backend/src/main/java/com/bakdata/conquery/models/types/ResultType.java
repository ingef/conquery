package com.bakdata.conquery.models.types;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiFunction;

import c10n.C10N;
import com.bakdata.conquery.internationalization.Results;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public abstract class ResultType<T> {

	public static ResultType<?> resolveResultType(MajorTypeId majorTypeId) {
		return switch (majorTypeId) {
			case STRING -> StringT.INSTANCE;
			case BOOLEAN -> BooleanT.INSTANCE;
			case DATE -> DateT.INSTANCE;
			case DATE_RANGE -> DateRangeT.INSTANCE;
			case INTEGER -> IntegerT.INSTANCE;
			case MONEY -> MoneyT.INSTANCE;
			case DECIMAL, REAL -> NumericT.INSTANCE;
		};
	}

	public String printNullable(PrintSettings cfg, Object f) {
		if (f == null) {
			return "";
		}
		return print(cfg, f);
	}

	protected abstract String print(PrintSettings cfg, @NonNull Object f);

	public String toString() {
		return typeInfo();
	}

	public abstract String typeInfo();

	public abstract T getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException;

	protected abstract List<T> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException;

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class BooleanT extends ResultType<Boolean> {
		@Getter(onMethod_ = @JsonCreator)
		public static final BooleanT INSTANCE = new BooleanT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			Preconditions.checkArgument(f instanceof Boolean, "Expected boolean value, but got %s", f.getClass().getName());

			if (cfg.isPrettyPrint()) {
				//TODO this might be incredibly slow, probably better to cache this in the instance but we need to not use Singletons for that
				return (Boolean) f ? C10N.get(Results.class, cfg.getLocale()).True() : C10N.get(Results.class, cfg.getLocale()).False();
			}

			return (Boolean) f ? "1" : "0";
		}

		@Override
		public String typeInfo() {
			return "BOOLEAN";
		}

		@Override
		public Boolean getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getBoolean(resultSet, columnIndex);
		}

		@Override
		protected List<Boolean> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getBooleanList(resultSet, columnIndex);
		}
	}


	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class IntegerT extends ResultType<Integer> {
		@Getter(onMethod_ = @JsonCreator)
		public static final IntegerT INSTANCE = new IntegerT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getIntegerFormat().format(((Number) f).longValue());
			}
			return f.toString();
		}

		@Override
		public String typeInfo() {
			return "INTEGER";
		}

		@Override
		public Integer getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getInteger(resultSet, columnIndex);
		}

		@Override
		protected List<Integer> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getIntegerList(resultSet, columnIndex);
		}
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class NumericT extends ResultType<Double> {
		@Getter(onMethod_ = @JsonCreator)
		public static final NumericT INSTANCE = new NumericT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(f);
			}
			return f.toString();
		}

		@Override
		public String typeInfo() {
			return "NUMERIC";
		}

		@Override
		public Double getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getDouble(resultSet, columnIndex);
		}

		@Override
		protected List<Double> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getDoubleList(resultSet, columnIndex);
		}
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class DateT extends ResultType<Number> {
		@Getter(onMethod_ = @JsonCreator)
		public static final DateT INSTANCE = new DateT();

		@Override
		public String print(PrintSettings cfg, @NonNull Object f) {
			if (!(f instanceof Number)) {
				throw new IllegalStateException("Expected an Number but got an '" + f.getClass().getName() + "' with the value: " + f);
			}
			final Number number = (Number) f;
			return print(number, cfg.getDateFormatter());
		}

		public static String print(Number num, DateTimeFormatter formatter) {
			return formatter.format(LocalDate.ofEpochDay(num.intValue()));
		}

		@Override
		public String typeInfo() {
			return "DATE";
		}

		@Override
		public Number getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getDate(resultSet, columnIndex);
		}

		@Override
		protected List<Number> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getDateList(resultSet, columnIndex);
		}
	}

	/**
	 * A DateRange is provided by in a query result as two ints in a list, both standing for an epoch day (see {@link LocalDate#toEpochDay()}).
	 * The first int describes the included lower bound of the range. The second int descibes the included upper bound.
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class DateRangeT extends ResultType<List<Integer>> {
		@Getter(onMethod_ = @JsonCreator)
		public static final DateRangeT INSTANCE = new DateRangeT();

		@Override
		public String print(PrintSettings cfg, @NonNull Object f) {
			Preconditions.checkArgument(f instanceof List<?>, "Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f);

			final List<?> list = (List<?>) f;

			Preconditions.checkArgument(list.size() == 2, "Expected a list with 2 elements: one min, one max. The list was: %s", list);

			final DateTimeFormatter dateFormat = cfg.getDateFormatter();
			final Integer min = (Integer) list.get(0);
			final Integer max = (Integer) list.get(1);

			if (min == null || max == null) {
				log.warn("Encountered incomplete range, treating it as an open range. Either min or max was null: {}", list);
			}
			// Compute minString first because we need it either way
			final String minString = min == null || min == CDateRange.NEGATIVE_INFINITY ? "-∞" : ResultType.DateT.print(min, dateFormat);

			if (cfg.isPrettyPrint() && min != null && min.equals(max)) {
				// If the min and max are the same we print it like a singe date, not a range (only in pretty printing)
				return minString;
			}
			final String maxString = max == null || max == CDateRange.POSITIVE_INFINITY ? "+∞" : ResultType.DateT.print(max, dateFormat);

			return minString + cfg.getDateRangeSeparator() + maxString;
		}

		@Override
		public String typeInfo() {
			return "DATE_RANGE";
		}

		@Override
		public List<Integer> getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getDateRange(resultSet, columnIndex);
		}

		@Override
		public List<List<Integer>> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getDateRangeList(resultSet, columnIndex);
		}
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class StringT extends ResultType<String> {
		@Getter(onMethod_ = @JsonCreator)
		public static final StringT INSTANCE = new StringT();

		/**
		 * Function that allows a select to transform the internal value to an external representation.
		 * The returned value can be null.
		 */
		private BiFunction<Object, PrintSettings, String> valueMapper;

		public StringT(BiFunction<Object, PrintSettings, String> valueMapper) {
			this.valueMapper = valueMapper;
		}


		@Override
		protected String print(PrintSettings cfg, @NonNull Object f) {
			if (valueMapper == null) {
				return f.toString();
			}
			return valueMapper.apply(f, cfg);
		}

		@Override
		public String typeInfo() {
			return "STRING";
		}

		@Override
		public String getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getString(resultSet, columnIndex);
		}

		@Override
		protected List<String> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getStringList(resultSet, columnIndex);
		}
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class MoneyT extends ResultType<BigDecimal> {

		@Getter(onMethod_ = @JsonCreator)
		public static final MoneyT INSTANCE = new MoneyT();

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(readIntermediateValue(cfg, (Number) f));
			}
			return IntegerT.INSTANCE.print(cfg, f);
		}

		@NotNull
		public BigDecimal readIntermediateValue(PrintSettings cfg, Number f) {
			return new BigDecimal(f.longValue()).movePointLeft(cfg.getCurrency().getDefaultFractionDigits());
		}

		@Override
		public String typeInfo() {
			return "MONEY";
		}

		@Override
		public BigDecimal getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getMoney(resultSet, columnIndex);
		}

		@Override
		protected List<BigDecimal> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return resultSetProcessor.getMoneyList(resultSet, columnIndex);
		}
	}

	@Getter
	@EqualsAndHashCode(callSuper = false)
	public static class ListT<T> extends ResultType<List<T>> {

		@NonNull
		private final ResultType<T> elementType;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public ListT(@NonNull ResultType<T> elementType) {
			this.elementType = elementType;
		}

		@Override
		public String print(PrintSettings cfg, @NonNull Object f) {
			// Jackson deserializes collections as lists instead of an array, if the type is not given
			if (!(f instanceof List)) {
				throw new IllegalStateException(String.format("Expected a List got %s (Type: %s, as string: %s)", f, f.getClass().getName(), f));
			}
			// Not sure if this escaping is enough
			final LocaleConfig.ListFormat listFormat = cfg.getListFormat();
			final StringJoiner joiner = listFormat.createListJoiner();
			for (Object obj : (List<?>) f) {
				joiner.add(listFormat.escapeListElement(elementType.print(cfg, obj)));
			}
			return joiner.toString();
		}

		@Override
		public String typeInfo() {
			return "LIST[" + elementType.typeInfo() + "]";
		}

		@Override
		public List<T> getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return elementType.getFromResultSetAsList(resultSet, columnIndex, resultSetProcessor);
		}

		@Override
		protected List<List<T>> getFromResultSetAsList(final ResultSet resultSet, final int columnIndex, final ResultSetProcessor resultSetProcessor) {
			throw new UnsupportedOperationException("Nested lists not supported in SQL mode");
		}
	}
}
