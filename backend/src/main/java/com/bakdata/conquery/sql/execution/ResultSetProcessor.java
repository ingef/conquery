package com.bakdata.conquery.sql.execution;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface ResultSetProcessor {

	String getString(ResultSet resultSet, int columnIndex) throws SQLException;

	Integer getInteger(ResultSet resultSet, int columnIndex) throws SQLException;

	Double getDouble(ResultSet resultSet, int columnIndex) throws SQLException;

	BigDecimal getMoney(ResultSet resultSet, int columnIndex) throws SQLException;

	Boolean getBoolean(ResultSet resultSet, int columnIndex) throws SQLException;

	Number getDate(ResultSet resultSet, int columnIndex) throws SQLException;

	List<Integer> getDateRange(ResultSet resultSet, int columnIndex) throws SQLException;

	List<List<Integer>> getDateRangeList(ResultSet resultSet, int columnIndex) throws SQLException;

}
