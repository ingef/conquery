package com.bakdata.conquery.sql.execution;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
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
	private static final int VALUES_OFFSET_INDEX = 2;

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

		String sqlString = sqlQuery.getSqlQuery().getSql();
		List<ResultType> resultTypes = sqlQuery.getSqlQuery().getResultInfos().stream().map(ResultInfo::getType).toList();

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
		ResultSetProcessor.ResultSetMapper[] mappers = ResultSetProcessor.getMappers(resultTypes);
		while (resultSet.next()) {
			SqlEntityResult resultRow = getResultRow(resultSet, mappers, columnCount);
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

	private SqlEntityResult getResultRow(ResultSet resultSet, ResultSetProcessor.ResultSetMapper[] mappers, int columnCount) throws SQLException {

		int rowNumber = resultSet.getRow();
		String id = resultSet.getString(PID_COLUMN_INDEX);
		Object[] resultRow = new Object[columnCount - 1];

		for (int resultSetIndex = VALUES_OFFSET_INDEX; resultSetIndex <= columnCount; resultSetIndex++) {
			int resultTypeIndex = resultSetIndex - VALUES_OFFSET_INDEX;
			resultRow[resultTypeIndex] = mappers[resultTypeIndex].getFromResultSet(resultSet, resultSetIndex, this.resultSetProcessor);
		}

		return new SqlEntityResult(rowNumber, id, resultRow);
	}

}
