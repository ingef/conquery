package com.bakdata.conquery.sql.execution;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.ExecuteListener;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;


@Slf4j
@Data
public class SqlExecutionService {

	private static final int PID_COLUMN_INDEX = 1;
	private static final int VALUES_OFFSET_INDEX = 2;

	@Getter
	private final DSLContext dslContext;

	private final ResultSetProcessor resultSetProcessor;

	public DSLContext withMetadata(Map<String, String> metadata) {
		final String metadataComments = createMetadataComments(metadata);
		return withMetadataComments(metadataComments);
	}

	private DSLContext withMetadataComments(String metadataComments) {
		if (metadataComments.isEmpty()) {
			return dslContext;
		}

		final ExecuteListener metadataListener = ExecuteListener.onRenderEnd(context -> context.sql(metadataComments + context.sql()));
		return dslContext.configuration().deriveAppending(metadataListener).dsl();
	}

	public SqlExecutionExecutionInfo execute(SqlQuery sqlQuery, Map<String, String> metadata) {

		final String metadataComments = createMetadataComments(metadata);
		final SqlExecutionExecutionInfo result = createStatementAndExecute(sqlQuery, withMetadataComments(metadataComments), metadataComments);

		return result;
	}

	private static String createMetadataComments(Map<String, String> metadata) {
		Objects.requireNonNull(metadata, "metadata");

		return metadata.entrySet().stream()
					   .map(entry -> Map.entry(
							   Objects.requireNonNull(entry.getKey(), "metadata key"),
							   Objects.requireNonNull(entry.getValue(), "metadata value")
					   ))
					   .sorted(Map.Entry.comparingByKey())
					   .map(entry -> "-- %s=%s\n".formatted(escapeLineBreaks(entry.getKey()), escapeLineBreaks(entry.getValue())))
					   .collect(Collectors.joining());
	}

	private static String escapeLineBreaks(String value) {
		return value.replace("\r", "\\r").replace("\n", "\\n");
	}

	private SqlExecutionExecutionInfo createStatementAndExecute(SqlQuery sqlQuery, DSLContext executionContext, String metadataComments) {

		final String sqlString = sqlQuery.getSql();
		final List<ResultInfo> resultInfos = sqlQuery.getResultInfos();
		final List<ResultSetProcessor.Reader<?>> resultTypes = resultInfos.stream()
														.map(resultInfo -> resultInfo.createReader(resultSetProcessor))
														.collect(Collectors.toList());

		log.info("Executing query: \n{}{}", metadataComments, sqlString);

		try (ResultSet resultSet = executionContext.resultQuery(DSL.raw(sqlString)).fetchResultSet()) {
			final int columnCount = resultSet.getMetaData().getColumnCount();
			final List<String> columnNames = getColumnNames(resultSet, columnCount);
			final List<EntityResult> resultTable = createResultTable(resultSet, resultTypes, columnCount);

			return new SqlExecutionExecutionInfo(ExecutionState.RUNNING, columnNames, resultTable, resultInfos, new CountDownLatch(1));
		}
		// not all DB vendors throw SQLExceptions
		catch (SQLException | RuntimeException e) {
			log.error("Query execution failed", e);
			throw new ConqueryError.SqlError(e);
		}
	}

	private List<String> getColumnNames(ResultSet resultSet, int columnCount) {
		// JDBC ResultSet indices start with 1
		return IntStream.rangeClosed(1, columnCount)
						.mapToObj(columnIndex -> getColumnName(resultSet, columnIndex))
						.toList();
	}

	private List<EntityResult> createResultTable(ResultSet resultSet, List<ResultSetProcessor.Reader<?>> resultTypes, int columnCount) throws SQLException {
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

	private SqlEntityResult getResultRow(ResultSet resultSet, List<ResultSetProcessor.Reader<?>> resultTypes, int columnCount) throws SQLException {

		final String id = resultSet.getString(PID_COLUMN_INDEX);
		final Object[] resultRow = new Object[columnCount - 1];

		for (int resultSetIndex = VALUES_OFFSET_INDEX; resultSetIndex <= columnCount; resultSetIndex++) {
			final int resultTypeIndex = resultSetIndex - VALUES_OFFSET_INDEX;
			resultRow[resultTypeIndex] = resultTypes.get(resultTypeIndex).read(resultSet, resultSetIndex);
		}

		return new SqlEntityResult(id, resultRow);
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
