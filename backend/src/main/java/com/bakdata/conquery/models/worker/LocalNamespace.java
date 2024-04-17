package com.bakdata.conquery.models.worker;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.local.SqlStorageHandler;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@Getter
public class LocalNamespace extends Namespace {

	private final SqlExecutionService sqlExecutionService;

	private final SqlStorageHandler storageHandler;

	public LocalNamespace(
			ObjectMapper preprocessMapper,
			ObjectMapper communicationMapper,
			NamespaceStorage storage,
			ExecutionManager executionManager,
			SqlExecutionService sqlExecutionService,
			JobManager jobManager,
			FilterSearch filterSearch,
			IndexService indexService,
			List<Injectable> injectables
	) {
		super(preprocessMapper, communicationMapper, storage, executionManager, jobManager, filterSearch, indexService, injectables);
		this.sqlExecutionService = sqlExecutionService;
		this.storageHandler = new SqlStorageHandler(sqlExecutionService);
	}

	@Override
	void updateMatchingStats() {
		// TODO Build basic statistic on data
	}

	@Override
	void registerColumnValuesInSearch(Set<Column> columns) {
		for (Column column : columns) {
			final Stream<String> stringStream = storageHandler.lookupColumnValues(getStorage(), column);
			getFilterSearch().registerValues(column, stringStream.collect(Collectors.toSet()));
		}
	}
}
