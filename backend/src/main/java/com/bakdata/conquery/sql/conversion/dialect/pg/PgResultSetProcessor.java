package com.bakdata.conquery.sql.conversion.dialect.pg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import com.bakdata.conquery.util.DateReader;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;

@RequiredArgsConstructor
public class PgResultSetProcessor implements ResultSetProcessor {

	protected final ConqueryConfig config;
	protected final SqlCDateSetParser dateSetParser;

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
	public Integer getDate(ResultSet resultSet, int columnIndex) throws SQLException {
		String dateString = resultSet.getString(columnIndex);
		if (dateString == null) {
			return null;
		}
		DateReader dateReader = config.getLocale().getDateReader();
		return (int) dateReader.parseToLocalDate(dateString).toEpochDay();
	}

	@Override
	public List<Integer> getDateRange(ResultSet resultSet, int columnIndex) throws SQLException {
		return this.dateSetParser.toEpochDayRange(resultSet.getString(columnIndex));
	}

	@Override
	public List<List<Integer>> getDateRangeList(ResultSet resultSet, int columnIndex) throws SQLException {
		return this.dateSetParser.toEpochDayRangeList(resultSet.getString(columnIndex));
	}

	@Override
	public List<String> getStringList(ResultSet resultSet, int columnIndex) throws SQLException {
		return list(resultSet, columnIndex);
	}

	@Override
	public List<Boolean> getBooleanList(ResultSet resultSet, int columnIndex) throws SQLException {
		return list(resultSet, columnIndex);
	}

	@Override
	public List<Integer> getIntegerList(ResultSet resultSet, int columnIndex) throws SQLException {
		return list(resultSet, columnIndex);
	}

	@Override
	public List<Double> getDoubleList(ResultSet resultSet, int columnIndex) throws SQLException {
		return list(resultSet, columnIndex);
	}

	@Override
	public List<BigDecimal> getMoneyList(ResultSet resultSet, int columnIndex) throws SQLException {
		return list(
				resultSet,
				columnIndex
		);
	}

	@Override
	public List<Number> getDateList(ResultSet resultSet, int columnIndex) throws SQLException {
		return list(resultSet, columnIndex);
	}

	private <T> List<T> list(ResultSet resultSet, int columnIndex) throws SQLException {
		Array arrayExpression = resultSet.getArray(columnIndex);
		if (arrayExpression == null) {
			return null;
		}

		List<T> result = Arrays.stream(((Object[]) arrayExpression.getArray()))
							   .filter(Objects::nonNull)
							   .filter(obj -> !(obj instanceof String str) || Strings.isNotBlank(str))
							   .map(o -> (T) o)
							   .toList();

		return result.isEmpty() ? null : result;
	}

	@FunctionalInterface
	private interface Getter {
		Object getFromResultSet(int columnIndex) throws SQLException;
	}

}
