package com.bakdata.conquery.sql.execution;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.types.ResultType;

public interface ResultSetProcessor {

	Map<ResultType, ResultSetMapper> MAPPERS = createMappers();

	String getString(ResultSet resultSet, int columnIndex) throws SQLException;

	Integer getInteger(ResultSet resultSet, int columnIndex) throws SQLException;

	Double getDouble(ResultSet resultSet, int columnIndex) throws SQLException;

	BigDecimal getMoney(ResultSet resultSet, int columnIndex) throws SQLException;

	Boolean getBoolean(ResultSet resultSet, int columnIndex) throws SQLException;

	Number getDate(ResultSet resultSet, int columnIndex) throws SQLException;

	List<Integer> getDateRange(ResultSet resultSet, int columnIndex) throws SQLException;

	List<List<Integer>> getDateRangeList(ResultSet resultSet, int columnIndex) throws SQLException;

	@FunctionalInterface
	interface ResultSetMapper {
		Object getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException;
	}

	static ResultSetMapper getMapper(ResultType resultType) {
		if (resultType instanceof ResultType.ListT list) {
			ResultType elementType = list.getElementType();
			if (elementType instanceof ResultType.DateRangeT) {
				return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDateRangeList(resultSet, columnIndex);
			}
			return MAPPERS.get(elementType);
		}
		return MAPPERS.get(resultType);
	}

	private static Map<ResultType, ResultSetMapper> createMappers() {
		return Map.of(
				ResultType.StringT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getString(resultSet, columnIndex),
				ResultType.IntegerT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getInteger(resultSet, columnIndex),
				ResultType.NumericT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDouble(resultSet, columnIndex),
				ResultType.MoneyT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getMoney(resultSet, columnIndex),
				ResultType.BooleanT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getBoolean(resultSet, columnIndex),
				ResultType.DateT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDate(resultSet, columnIndex),
				ResultType.DateRangeT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDateRange(resultSet, columnIndex)
		);
	}

}
