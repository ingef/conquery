package com.bakdata.conquery.sql.execution;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.bakdata.conquery.models.common.CDateSet;

class DefaultResultSetProcessor implements ResultSetProcessor {

	private final CDateSetParser cDateSetParser;

	public DefaultResultSetProcessor(CDateSetParser cDateSetParser) {
		this.cDateSetParser = cDateSetParser;
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
	public Boolean getBoolean(ResultSet resultSet, int columnIndex) throws SQLException {
		return checkForNullElseGet(resultSet, columnIndex, resultSet::getBoolean, Boolean.class);
	}

	@Override
	public CDateSet getCDateSet(ResultSet resultSet, int columnIndex) throws SQLException {
		return this.cDateSetParser.fromString(resultSet.getString(columnIndex));
	}

	@FunctionalInterface
	private interface Getter {
		Object getFromResultSet(int columnIndex) throws SQLException;
	}

	private static <T> T checkForNullElseGet(ResultSet resultSet, int columnIndex, Getter getter, Class<T> resultType) throws SQLException {
		// e.g. calling a primitives' ResultSet getter like getDouble, getInt etc. straightaway will never return null
		if (resultSet.getObject(columnIndex) == null) {
			return null;
		}
		return resultType.cast(getter.getFromResultSet(columnIndex));
	}

}
