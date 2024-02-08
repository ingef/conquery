package com.bakdata.conquery.models.worker;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@Getter
public class LocalNamespace extends Namespace {

	private final SqlExecutionService sqlExecutionService;

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
	}

	@Override
	void updateFilterSearch() {
		getJobManager().addSlowJob(new UpdateFilterSearchJob(this, getFilterSearch().getIndexConfig(), this::buildSearchForColumnValuesAsync));
	}

	@Override
	void updateMatchingStats() {
		// TODO Build basic statistic on data
	}

	@Override
	void buildSearchForColumnValuesAsync(Set<Column> columns) {
		// TODO Gather values for these columns and register them
	}

	@Override
	public void postprocessData() {
		// TODO Not implemented yet
	}
}
