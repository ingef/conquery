package com.bakdata.conquery.sql.execution;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
import com.google.common.base.Stopwatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@RequiredArgsConstructor
@Slf4j
public class SqlExecutionService {

	private static final int PID_COLUMN_INDEX = 1;
	private static final int VALIDITY_DATE_COLUMN_INDEX = 2;
	private static final int VALUES_OFFSET_INDEX = 3;

	private final DSLContext dslContext;
	private final ResultSetProcessor resultSetProcessor;

	public SqlExecutionResult execute(SqlManagedQuery sqlQuery) {
		log.info("Starting SQL execution[{}]", sqlQuery.getQueryId());
		Stopwatch stopwatch = Stopwatch.createStarted();
		SqlExecutionResult result = dslContext.connectionResult(connection -> createStatementAndExecute(sqlQuery, connection));
		log.info("Finished SQL execution[{}] with {} results within {}", sqlQuery.getQueryId(), result.getRowCount(), stopwatch.elapsed());
		return result;
	}

	private SqlExecutionResult createStatementAndExecute(SqlManagedQuery sqlQuery, Connection connection) {

		String sqlString = sqlQuery.getSqlQuery().toString();
		List<ResultType> resultTypes = sqlQuery.getSqlQuery().getResultTypes();

		log.debug("Executing query: \n{}", sqlString);
		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(sqlString)) {
			int columnCount = resultSet.getMetaData().getColumnCount();
			List<String> columnNames = getColumnNames(resultSet, columnCount);
			List<EntityResult> resultTable = createResultTable(resultSet, resultTypes, columnCount);

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

	private List<EntityResult> createResultTable(ResultSet resultSet, List<ResultType> resultTypes, int columnCount) throws SQLException {
		List<EntityResult> resultTable = new ArrayList<>(resultSet.getFetchSize());
		while (resultSet.next()) {
			SqlEntityResult resultRow = getResultRow(resultSet, resultTypes, columnCount);
			resultTable.add(resultRow);
		}
		return resultTable;
	}

	private List<String> getColumnNames(ResultSet resultSet, int columnCount) {
		// JDBC ResultSet indices start with 1
		return IntStream.rangeClosed(1, columnCount)
						.mapToObj(columnIndex -> getColumnName(resultSet, columnIndex))
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

	private SqlEntityResult getResultRow(ResultSet resultSet, List<ResultType> resultTypes, int columnCount) throws SQLException {

		int rowNumber = resultSet.getRow();
		String id = resultSet.getObject(PID_COLUMN_INDEX).toString();
		Object[] resultRow = new Object[columnCount - 1];

		CDateSet validityDate = this.resultSetProcessor.getCDateSet(resultSet, VALIDITY_DATE_COLUMN_INDEX);
		resultRow[0] = validityDate;

		int resultTypeIndex = 0;
		for (int resultSetIndex = VALUES_OFFSET_INDEX; resultSetIndex <= columnCount; resultSetIndex++) {
			int resultRowOffset = resultTypeIndex + 1;    // validity date is part of result row, but not part of the result type list
			resultRow[resultRowOffset] = ResultSetProcessor.getMapper(resultTypes.get(resultTypeIndex))
														   .getFromResultSet(resultSet, resultSetIndex, this.resultSetProcessor);
			resultTypeIndex++;
		}

		return new SqlEntityResult(rowNumber, id, resultRow);
	}

}
