package com.bakdata.conquery.sql.execution;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface ResultSetProcessor {

	char UNIT_SEPARATOR = (char) 31; // https://www.ascii-code.com/character/%E2%90%9F

	String getString(ResultSet resultSet, int columnIndex) throws SQLException;

	Integer getInteger(ResultSet resultSet, int columnIndex) throws SQLException;

	Double getDouble(ResultSet resultSet, int columnIndex) throws SQLException;

	BigDecimal getMoney(ResultSet resultSet, int columnIndex) throws SQLException;

	Boolean getBoolean(ResultSet resultSet, int columnIndex) throws SQLException;

	Integer getDate(ResultSet resultSet, int columnIndex) throws SQLException;

	List<Integer> getDateRange(ResultSet resultSet, int columnIndex) throws SQLException;

	List<List<Integer>> getDateRangeList(ResultSet resultSet, int columnIndex) throws SQLException;

	List<String> getStringList(ResultSet resultSet, int columnIndex) throws SQLException;

	List<Boolean> getBooleanList(ResultSet resultSet, int columnIndex) throws SQLException;

	List<Integer> getIntegerList(ResultSet resultSet, int columnIndex) throws SQLException;

	List<Double> getDoubleList(ResultSet resultSet, int columnIndex) throws SQLException;

	List<BigDecimal> getMoneyList(ResultSet resultSet, int columnIndex) throws SQLException;

	List<Number> getDateList(ResultSet resultSet, int columnIndex) throws SQLException;
}
