package com.bakdata.conquery.mode.local;

import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.StorageHandler;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.impl.DSL;

@Slf4j
@RequiredArgsConstructor
public class SqlStorageHandler implements StorageHandler {

	private final SqlExecutionService sqlExecutionService;

	@Override
	public Stream<String> lookupColumnValues(NamespaceStorage namespaceStorage, Column column) {
		Select<Record1<Object>> columValuesQuery = DSL.selectDistinct(DSL.field(DSL.name(column.getName())))
													  .from(DSL.table(DSL.name(column.getTable().getName())));
		return queryForDistinctValues(columValuesQuery);
	}

	private Stream<String> queryForDistinctValues(Select<Record1<Object>> columValuesQuery) {
		try {
			return sqlExecutionService.fetchStream(columValuesQuery)
									  .map(record -> record.get(0, String.class))
									  // the database might return null or an empty string as a distinct value
									  .filter(value -> value != null && !value.isBlank());
		}
		catch (Exception e) {
			log.error("Expecting exactly 1 column in Result when querying for distinct values of a column. Query: {}.", columValuesQuery, e);
		}
		return Stream.empty();
	}

}
