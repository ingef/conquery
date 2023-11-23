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

	static ResultSetMapper[] getMappers(List<ResultType> resultTypes) {
		return resultTypes.stream()
						  .map(ResultSetProcessor::getMappers)
						  .toArray(ResultSetProcessor.ResultSetMapper[]::new);
	}

private static ResultSetMapper getMappers(ResultType resultType) {
		if (resultType instanceof ResultType.ListT list) {
			ResultType elementType = list.getElementType();
			if (elementType instanceof ResultType.DateRangeT) {
				return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDateRangeList(resultSet, columnIndex);
			}
			return getMappers(elementType);
		}

		if (resultType instanceof StringT){
			// TODO mapping should probably be applied in query when using SQL-backend
			return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getString(resultSet, columnIndex);
		}

		if (resultType instanceof IntegerT){
			return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getInteger(resultSet, columnIndex);
		}

		if (resultType instanceof NumericT){
			return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDoble(resultSet, columnIndex);
		}

		if (resultType instanceof MoneyT){
			//TODO money needs formatting according to pretty printer?
			return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getMoney(resultSet, columnIndex);
		}

		if (resultType instanceof BooleanT){
			return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getBoolean(resultSet, columnIndex);
		}

		if (resultType instanceof DateT){
			return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDate(resultSet, columnIndex);
		}

		if (resultType instanceof DateRangeT){
			return (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDateRange(resultSet, columnIndex);
		}
		
		
		throw new IllegalArgumentException("Don't know how to handle result Type %s".formatted(resultType));
	}

}
