package com.bakdata.conquery.sql.execution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
		try {
			Array result = resultSet.getArray(columnIndex);
			// ResultSet does not provide a way to directly get an array of a specific type (see https://docs.oracle.com/javase/tutorial/jdbc/basics/array.html)
			String[] casted = (String[]) result.getArray();
			return Arrays.stream(casted).filter(Objects::nonNull).toList();
		}
		catch (ClassCastException exception) {
			throw new SQLException("Expected an array of type String at column index %s in ResultSet %s.".formatted(columnIndex, resultSet));
		}
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
