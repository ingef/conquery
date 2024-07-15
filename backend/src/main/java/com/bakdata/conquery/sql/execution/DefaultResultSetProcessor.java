package com.bakdata.conquery.sql.execution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class DefaultResultSetProcessor implements ResultSetProcessor {

	private final SqlCDateSetParser sqlCDateSetParser;

	@Override
	public String getString(ResultSet resultSet, int columnIndex) throws SQLException {
		return resultSet.getString(columnIndex);
	}

	@Override
	public Integer getInteger(ResultSet resultSet, int columnIndex) throws SQLException {
		return checkForNullElseGet(resultSet, columnIndex, resultSet::getInt, Integer.class);
	}

	@Override
	public Double getDouble(ResultSet resultSet, int columnIndex) throws SQLException {
		return checkForNullElseGet(resultSet, columnIndex, resultSet::getDouble, Double.class);
	}

	@Override
	public BigDecimal getMoney(ResultSet resultSet, int columnIndex) throws SQLException {
		BigDecimal money = resultSet.getBigDecimal(columnIndex);
		if (money == null) {
			return null;
		}
		return money.setScale(2, RoundingMode.HALF_EVEN);
	}

	@Override
	public Boolean getBoolean(ResultSet resultSet, int columnIndex) throws SQLException {
		return checkForNullElseGet(resultSet, columnIndex, resultSet::getBoolean, Boolean.class);
	}

	@Override
	public Number getDate(ResultSet resultSet, int columnIndex) throws SQLException {
		Date date = resultSet.getDate(columnIndex);
		if (date == null) {
			return null;
		}
		return date.toLocalDate().toEpochDay();
	}

	@Override
	public List<Integer> getDateRange(ResultSet resultSet, int columnIndex) throws SQLException {
		return this.sqlCDateSetParser.toEpochDayRange(resultSet.getString(columnIndex));
	}

	@Override
	public List<List<Integer>> getDateRangeList(ResultSet resultSet, int columnIndex) throws SQLException {
		return this.sqlCDateSetParser.toEpochDayRangeList(resultSet.getString(columnIndex));
	}

	@Override
	public List<String> getStringList(ResultSet resultSet, int columnIndex) throws SQLException {
		return fromString(resultSet, columnIndex, (string) -> string);
	}

	@Override
	public List<Boolean> getBooleanList(ResultSet resultSet, int columnIndex) throws SQLException {
		return fromString(resultSet, columnIndex, Boolean::valueOf);
	}

	@Override
	public List<Integer> getIntegerList(ResultSet resultSet, int columnIndex) throws SQLException {
		return fromString(resultSet, columnIndex, Integer::valueOf);
	}

	@Override
	public List<Double> getDoubleList(ResultSet resultSet, int columnIndex) throws SQLException {
		return fromString(resultSet, columnIndex, Double::valueOf);
	}

	@Override
	public List<BigDecimal> getMoneyList(ResultSet resultSet, int columnIndex) throws SQLException {
		return fromString(
				resultSet,
				columnIndex,
				(string) -> BigDecimal.valueOf(Double.parseDouble(string))
									  .setScale(2, RoundingMode.HALF_EVEN)
		);
	}

	@Override
	public List<Number> getDateList(ResultSet resultSet, int columnIndex) throws SQLException {
		return fromString(resultSet, columnIndex, (string) -> Date.valueOf(string).toLocalDate().toEpochDay());
	}

	private <T> List<T> fromString(ResultSet resultSet, int columnIndex, Function<String, T> parseFunction) throws SQLException {
		String arrayExpression = resultSet.getString(columnIndex);
		if (arrayExpression == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(arrayExpression.split(String.valueOf(ResultSetProcessor.UNIT_SEPARATOR)))
					 .filter(Predicate.not(String::isBlank))
					 .map(parseFunction)
					 .toList();
	}

	@FunctionalInterface
	private interface Getter {
		Object getFromResultSet(int columnIndex) throws SQLException;
	}

	/**
	 * Use to keep null values for primitive data types.
	 * <p>
	 * For example, calling a primitives' ResultSet getter like getDouble, getInt etc. straightaway will never return null.
	 */
	private static <T> T checkForNullElseGet(ResultSet resultSet, int columnIndex, Getter getter, Class<T> resultType) throws SQLException {
		if (resultSet.getObject(columnIndex) == null) {
			return null;
		}
		return resultType.cast(getter.getFromResultSet(columnIndex));
	}

}
