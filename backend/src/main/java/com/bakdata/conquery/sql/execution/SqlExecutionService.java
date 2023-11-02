package com.bakdata.conquery.sql.execution;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
import com.google.common.base.Stopwatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@RequiredArgsConstructor
@Slf4j
public class SqlExecutionService {

	private final DSLContext dslContext;

	public SqlExecutionResult execute(SqlManagedQuery sqlQuery) {
		log.info("Starting SQL execution[{}]", sqlQuery.getQueryId());
		Stopwatch stopwatch = Stopwatch.createStarted();
		SqlExecutionResult result = dslContext.connectionResult(connection -> this.createStatementAndExecute(sqlQuery, connection));
		log.info("Finished SQL execution[{}] with {} results within {}", sqlQuery.getQueryId(), result.getRowCount(), stopwatch.elapsed());
		return result;
	}

	private SqlExecutionResult createStatementAndExecute(SqlManagedQuery sqlQuery, Connection connection) {

		String sqlString = sqlQuery.getSqlQuery().getSqlString();
		log.debug("Executing query: \n{}", sqlString);
		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(sqlString)) {
			int columnCount = resultSet.getMetaData().getColumnCount();
			List<String> columnNames = this.getColumnNames(resultSet, columnCount);
			List<EntityResult> resultTable = this.createResultTable(resultSet, columnCount);

			return new SqlExecutionResult(columnNames, resultTable);
		}
		catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
		}
		// not all DB vendors throw SQLExceptions
		catch (RuntimeException e) {
			throw new ConqueryError.SqlError(new SQLException(e));
		}
	}

	private List<EntityResult> createResultTable(ResultSet resultSet, int columnCount) throws SQLException {
		List<EntityResult> resultTable = new ArrayList<>(resultSet.getFetchSize());
		while (resultSet.next()) {
			Object[] resultRow = this.getResultRow(resultSet, columnCount);
			resultTable.add(new SqlEntityResult(resultSet.getRow(), resultSet.getObject(1).toString(), resultRow));
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
