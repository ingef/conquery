package com.bakdata.conquery.mode.local;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.StorageHandler;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.impl.DSL;

@Slf4j
@RequiredArgsConstructor
public class SqlStorageHandler implements StorageHandler {

	private final SqlExecutionService sqlExecutionService;

	@Override
	public List<String> lookupColumnValues(NamespaceStorage namespaceStorage, Column column) {
		Select<Record1<Object>> columValuesQuery = DSL.selectDistinct(DSL.field(DSL.name(column.getName())))
													  .from(DSL.table(DSL.name(column.getTable().getName())));
		return queryForDistinctValues(columValuesQuery);
	}

	private List<String> queryForDistinctValues(Select<Record1<Object>> columValuesQuery) {
		Result<?> result = sqlExecutionService.execute(columValuesQuery);
		try {
			return result.getValues(0, String.class).stream()
						 // the database might return null or an empty string as a distinct value
						 .filter(value -> value != null && !value.isEmpty())
						 .toList();
		}
		catch (Exception e) {
			log.error("Expecting exactly 1 column in Result when querying for distinct values of a column. Query: {}. Error: {}", columValuesQuery, e);
		}
		return Collections.emptyList();
	}

}
