package com.bakdata.conquery.sql.execution;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.sql.SqlQuery;
import org.jooq.DSLContext;

public class SqlExecutionService {

	private final DSLContext dslContext;

	public SqlExecutionService(DSLContext dslContext) {
		this.dslContext = dslContext;
	}

	public SqlExecutionResult execute(SqlQuery sqlQuery) {
		return dslContext.connectionResult(connection -> this.createStatementAndExecute(sqlQuery.getSqlString(), connection));
	}

	private SqlExecutionResult createStatementAndExecute(String sqlQuery, Connection connection) {

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(sqlQuery)) {
			int columnCount = resultSet.getMetaData().getColumnCount();
			List<String> columnNames = this.getColumnNames(resultSet, columnCount);
			List<SinglelineEntityResult> resultTable = this.createResultTable(resultSet, columnCount);

			return new SqlExecutionResult(columnNames, resultTable);

		}
		catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
		}
	}

	private List<SinglelineEntityResult> createResultTable(ResultSet resultSet, int columnCount) throws SQLException {
		List<SinglelineEntityResult> resultTable = new ArrayList<>(resultSet.getFetchSize());
		while (resultSet.next()) {
			Object[] resultRow = this.getResultRow(resultSet, columnCount);
			resultTable.add(new SinglelineEntityResult(resultSet.getInt(1), resultRow));
		}
		return resultTable;
	}

	private List<String> getColumnNames(ResultSet resultSet, int columnCount) {
		// JDBC ResultSet indices start with 1
		return IntStream.rangeClosed(2, columnCount)
						.mapToObj(columnIndex -> this.getColumnName(resultSet, columnIndex))
						.toList();
	}

	private String getColumnName(ResultSet resultSet, int columnIndex) {
		try {
			return resultSet.getMetaData().getColumnName(columnIndex);
		}
		catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
		}
	}

	private Object[] getResultRow(ResultSet resultSet, int columnCount) {
		// JDBC ResultSet indices start with 1 and we skip the first column because it contains the id
		return IntStream.rangeClosed(2, columnCount)
						.mapToObj(columnIndex -> this.getValueOfColumn(resultSet, columnIndex))
						.toArray();
	}

	private String getValueOfColumn(ResultSet resultSet, int columnIndex) {
		try {
			return resultSet.getString(columnIndex);
		}
		catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
		}
	}

}
