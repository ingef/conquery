package com.bakdata.conquery.sql.execution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.exception.DataAccessException;


@Slf4j
@Data
public class SqlExecutionService {

	private static final int PID_COLUMN_INDEX = 1;
	private static final int VALUES_OFFSET_INDEX = 2;

	@Getter
	private final DSLContext dslContext;

	private final ResultSetProcessor resultSetProcessor;

	public SqlExecutionExecutionInfo execute(SqlQuery sqlQuery) {

		final SqlExecutionExecutionInfo result = dslContext.connectionResult(connection -> createStatementAndExecute(sqlQuery, connection));

		return result;
	}

	private SqlExecutionExecutionInfo createStatementAndExecute(SqlQuery sqlQuery, Connection connection) {

		final String sqlString = sqlQuery.getSql();
		final List<ResultType> resultTypes = sqlQuery.getResultInfos().stream().map(ResultInfo::getType).collect(Collectors.toList());

		log.info("Executing query: \n{}", sqlString);

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(sqlString)) {
			final int columnCount = resultSet.getMetaData().getColumnCount();
			final List<String> columnNames = getColumnNames(resultSet, columnCount);
			final List<EntityResult> resultTable = createResultTable(resultSet, resultTypes, columnCount);

			return new SqlExecutionExecutionInfo(ExecutionState.RUNNING, columnNames, resultTable, new CountDownLatch(1));
		}
		// not all DB vendors throw SQLExceptions
		catch (SQLException | RuntimeException e) {
			throw new ConqueryError.SqlError(e);
		}
	}

	private List<String> getColumnNames(ResultSet resultSet, int columnCount) {
		// JDBC ResultSet indices start with 1
		return IntStream.rangeClosed(1, columnCount)
						.mapToObj(columnIndex -> getColumnName(resultSet, columnIndex))
						.toList();
	}

	private List<EntityResult> createResultTable(ResultSet resultSet, List<ResultType> resultTypes, int columnCount) throws SQLException {
		final List<EntityResult> resultTable = new ArrayList<>(resultSet.getFetchSize());
		while (resultSet.next()) {
			final SqlEntityResult resultRow = getResultRow(resultSet, resultTypes, columnCount);
			resultTable.add(resultRow);
		}
		return resultTable;
	}

	private String getColumnName(ResultSet resultSet, int columnIndex) {
		try {
			return resultSet.getMetaData().getColumnName(columnIndex);
		}
		catch (SQLException e) {
			throw new ConqueryError.SqlError(e);
		}
	}

	private SqlEntityResult getResultRow(ResultSet resultSet, List<ResultType> resultTypes, int columnCount) throws SQLException {

		final String id = resultSet.getString(PID_COLUMN_INDEX);
		final Object[] resultRow = new Object[columnCount - 1];

		for (int resultSetIndex = VALUES_OFFSET_INDEX; resultSetIndex <= columnCount; resultSetIndex++) {
			final int resultTypeIndex = resultSetIndex - VALUES_OFFSET_INDEX;
			resultRow[resultTypeIndex] = resultTypes.get(resultTypeIndex).getFromResultSet(resultSet, resultSetIndex, resultSetProcessor);
		}

		return new SqlEntityResult(id, resultRow);
	}

	public Result<?> fetch(Select<?> query) {
		log.debug("Executing query: \n{}", query);
		try {
			return dslContext.fetch(query);
		}
		catch (DataAccessException exception) {
			throw new ConqueryError.SqlError(exception);
		}
	}

	/**
	 * Executes the query and returns the results as a Stream.
	 * <p>
	 * Note: The returned Stream is resourceful. It must be closed by the caller, because it contains a reference to an open {@link ResultSet}
	 * and {@link PreparedStatement}.
	 *
	 * @param query The query to be executed.
	 * @return A Stream of query results.
	 */
	public <R extends Record> Stream<R> fetchStream(Select<R> query) {
		log.debug("Executing query: \n{}", query);
		try {
			return dslContext.fetchStream(query);
		}
		catch (DataAccessException exception) {
			throw new ConqueryError.SqlError(exception);
		}
	}

}
