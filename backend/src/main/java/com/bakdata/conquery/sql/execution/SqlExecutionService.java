package com.bakdata.conquery.sql.execution;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.bakdata.conquery.models.error.ConqueryError;
import org.jooq.DSLContext;

public class SqlExecutionService {

    private final DSLContext dslContext;

    public SqlExecutionService(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public SqlExecutionResult execute(String sqlQuery) {
        return dslContext.connectionResult(connection -> this.createStatementAndExecute(sqlQuery, connection));
    }

	private SqlExecutionResult createStatementAndExecute(String sqlQuery, Connection connection) {

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(sqlQuery)) {
			int columnCount = resultSet.getMetaData().getColumnCount();
			List<String> columnNames = this.getColumnNames(resultSet, columnCount);
			List<List<String>> resultTable = this.createResultTable(resultSet, columnCount);

			return new SqlExecutionResult(columnNames, resultTable);

		} catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
		}
	}

	private List<List<String>> createResultTable(ResultSet resultSet, int columnCount) throws SQLException {
		List<List<String>> resultTable = new ArrayList<>(resultSet.getFetchSize());
		while (resultSet.next()) {
			resultTable.add(this.getResultRow(resultSet, columnCount));
		}
		return resultTable;
	}

	private List<String> getColumnNames(ResultSet resultSet, int columnCount) {
        // JDBC ResultSet indices start with 1
        return IntStream.rangeClosed(1, columnCount)
                .mapToObj(columnIndex -> this.getColumnName(resultSet, columnIndex))
                .toList();
    }

    private String getColumnName(ResultSet resultSet, int columnIndex) {
        try {
            return resultSet.getMetaData().getColumnName(columnIndex);
        } catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
        }
    }

    private List<String> getResultRow(ResultSet resultSet, int columnCount) {
        // JDBC ResultSet indices start with 1
        return IntStream.rangeClosed(1, columnCount)
                .mapToObj(columnIndex -> this.getValueOfColumn(resultSet, columnIndex))
                .toList();
    }

    private String getValueOfColumn(ResultSet resultSet, int columnIndex) {
        try {
            return resultSet.getString(columnIndex);
        } catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
        }
    }

}
