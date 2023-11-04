package com.bakdata.conquery.sql.execution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.types.ResultType;

public interface ResultSetProcessor {

	Map<ResultType, ResultSetMapper> MAPPERS = createMappers();

	String getString(ResultSet resultSet, int columnIndex) throws SQLException;

	Integer getInteger(ResultSet resultSet, int columnIndex) throws SQLException;

	Double getDouble(ResultSet resultSet, int columnIndex) throws SQLException;

	Boolean getBoolean(ResultSet resultSet, int columnIndex) throws SQLException;

	CDateSet getCDateSet(ResultSet resultSet, int columnIndex) throws SQLException;

	@FunctionalInterface
	interface ResultSetMapper {
		Object getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException;
	}

	static ResultSetMapper getMapper(ResultType resultType) {
		if (resultType instanceof ResultType.ListT list) {
			ResultType elementType = list.getElementType();
			return MAPPERS.get(elementType);
		}
		return MAPPERS.get(resultType);
	}

	// TODO @awildturtok: can we move this to ResultType class?
	private static Map<ResultType, ResultSetMapper> createMappers() {
		Map<ResultType, ResultSetMapper> mappers = new HashMap<>();
		mappers.put(ResultType.StringT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getString(resultSet, columnIndex));
		mappers.put(ResultType.IntegerT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getInteger(resultSet, columnIndex));
		mappers.put(ResultType.NumericT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getDouble(resultSet, columnIndex));
		// todo: after we use the printer, call getBoolean() instead of getInteger()
		mappers.put(ResultType.BooleanT.INSTANCE, (resultSet, columnIndex, resultSetProcessor) -> resultSetProcessor.getInteger(resultSet, columnIndex));
		return mappers;
	}

}
