package com.bakdata.conquery.models.worker;

import java.util.List;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@Getter
public class LocalNamespace extends Namespace {

	private final SqlExecutionService sqlExecutionService;
	private final SqlFunctionProvider functionProvider;

	public LocalNamespace(
			ObjectMapper preprocessMapper,
			ObjectMapper communicationMapper,
			NamespaceStorage storage,
			ExecutionManager executionManager,
			SqlExecutionService sqlExecutionService,
			SqlFunctionProvider functionProvider,
			JobManager jobManager,
			FilterSearch filterSearch,
			IndexService indexService,
			List<Injectable> injectables
	) {
		super(preprocessMapper, communicationMapper, storage, executionManager, jobManager, filterSearch, indexService, injectables);
		this.sqlExecutionService = sqlExecutionService;
		this.functionProvider = functionProvider;
	}
}
