package com.bakdata.conquery.sql.conversion.dialect.clickhouse;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.sql.execution.DefaultResultSetProcessor;
import com.google.common.base.Preconditions;

public class ClickhouseResultSetProcessor extends DefaultResultSetProcessor {

	public ClickhouseResultSetProcessor(ConqueryConfig config) {
		super(config, null);
	}

	@Override
	public List<Integer> getDateRange(ResultSet resultSet, int columnIndex) throws SQLException {
		Object daterange = resultSet.getObject(columnIndex);

		if (daterange == null) {
			return Collections.emptyList();
		}

		Object[] rawTuple = daterange instanceof Struct struct ? struct.getAttributes() : (Object[]) daterange;
		return toEpochDayRange(rawTuple);
	}

	public List<List<Integer>> toEpochDayRangeList(List<Object[]> raw) {
		if (raw == null) {
			return null;
		}

		List<List<Integer>> out = new ArrayList<>();

		for (Object[] rawTuple : raw) {
			List<Integer> dateRange = toEpochDayRange(rawTuple);
			if (!dateRange.isEmpty()) {
				out.add(dateRange);
			}
		}

		return out;
	}

	private List<Integer> toEpochDayRange(Object[] rawTuple) {
		Preconditions.checkArgument(rawTuple.length == 2, "Dateranges must have a start and end.");

		if (rawTuple[0] == null && rawTuple[1] == null) {
			return Collections.emptyList();
		}
		Preconditions.checkArgument(rawTuple[0] != null && rawTuple[1] != null, "Dateranges must have a start and end.");
		Preconditions.checkArgument(rawTuple[0] instanceof Number && rawTuple[1] instanceof Number, "Dateranges must contain integer values.");

		int begin = ((Number) rawTuple[0]).intValue();
		int end = ((Number) rawTuple[1]).intValue();

		if (begin == ClickhouseFunctionProvider.MIN_DATE_VALUE) {
			begin = CDateRange.NEGATIVE_INFINITY;
		}
		if (end == ClickhouseFunctionProvider.MAX_DATE_VALUE) {
			end = CDateRange.POSITIVE_INFINITY;
		}
		else {
			end--;
		}

		return List.of(begin, end);
	}

	private <T> List<T> sort(List<T> list, Comparator<T> sort) {
		if (list == null) {
			return null;
		}
		list.sort(sort);

		return list;
	}

	private <T> List<T> getList(ResultSet resultSet, int columnIndex) throws SQLException {
		Array sqlArray = resultSet.getArray(columnIndex);

		if (sqlArray == null) {
			return null;
		}

		Object[] array = (Object[]) sqlArray.getArray();

		List<T> out = new ArrayList<>();

		for (Object obj : array) {
			if (obj == null) {
				continue;
			}

			out.add((T) obj);
		}

		if (out.isEmpty()) {
			return null;
		}

		return out;
	}

	@Override
	public List<List<Integer>> getDateRangeList(ResultSet resultSet, int columnIndex) throws SQLException {
		List<Object[]> raw = getList(resultSet, columnIndex);

		return sort(this.toEpochDayRangeList(raw), Comparator.comparing(List::getFirst));
	}


	@Override
	public List<BigDecimal> getMoneyList(ResultSet resultSet, int columnIndex) throws SQLException {
		return sort(getList(resultSet, columnIndex), BigDecimal::compareTo);
	}

	@Override
	public List<Integer> getIntegerList(ResultSet resultSet, int columnIndex) throws SQLException {
		return sort(getList(resultSet, columnIndex), Integer::compareTo);
	}

	@Override
	public List<Double> getDoubleList(ResultSet resultSet, int columnIndex) throws SQLException {
		return sort(getList(resultSet, columnIndex), Double::compareTo);
	}

	@Override
	public List<Boolean> getBooleanList(ResultSet resultSet, int columnIndex) throws SQLException {
		return sort(getList(resultSet, columnIndex), Boolean::compareTo);
	}

	@Override
	public List<Number> getDateList(ResultSet resultSet, int columnIndex) throws SQLException {
		return sort(getList(resultSet, columnIndex), Comparator.comparing(Number::floatValue));
	}

	@Override
	public List<String> getStringList(ResultSet resultSet, int columnIndex) throws SQLException {
		return sort(getList(resultSet, columnIndex), String::compareTo);
	}
}
