package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.local.SqlEntityResolver;
import com.bakdata.conquery.mode.local.SqlStorageHandler;
import com.bakdata.conquery.mode.local.SqlUpdateMatchingStatsJob;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.sql.DSLContextWrapper;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class LocalNamespace extends Namespace {

	private final SqlConnectorConfig sqlConnectorConfig;
	private final DatabaseConfig databaseConfig;
	private final SqlDialect sqlDialect;
	private final SqlExecutionService sqlExecutionService;
	private final DSLContextWrapper dslContextWrapper;
	private final SqlStorageHandler storageHandler;

	public LocalNamespace(
			ObjectMapper preprocessMapper,
			NamespaceStorage storage,
			SqlConnectorConfig sqlConnectorConfig,
			DatabaseConfig databaseConfig,
			SqlDialect sqlDialect,
			SqlExecutionService sqlExecutionService,
			ExecutionManager executionManager,
			DSLContextWrapper dslContextWrapper,
			SqlStorageHandler storageHandler,
			JobManager jobManager,
			FilterSearch filterSearch,
			SqlEntityResolver sqlEntityResolver,
			List<Injectable> injectables
	) {
		super(preprocessMapper, storage, executionManager, jobManager, filterSearch, sqlEntityResolver, injectables);
		this.sqlConnectorConfig = sqlConnectorConfig;
		this.databaseConfig = databaseConfig;
		this.sqlDialect = sqlDialect;
		this.sqlExecutionService = sqlExecutionService;
		this.dslContextWrapper = dslContextWrapper;
		this.storageHandler = storageHandler;
	}

	@Override
	void updateMatchingStats() {
		final Set<ConceptId> concepts = collectConcepts();
		ExecutorService executorService = Executors.newFixedThreadPool(sqlConnectorConfig.getBackgroundThreads());
		Job job = new SqlUpdateMatchingStatsJob(
				databaseConfig,
				sqlExecutionService,
				sqlDialect.getFunctionProvider(),
				concepts,
				executorService
		);
		getJobManager().addSlowJob(job);
	}

	@Override
	void registerColumnValuesInSearch(Set<Column> columns) {
		for (Column column : columns) {
			try {
				final Stream<String> stringStream = storageHandler.lookupColumnValues(getStorage(), column);
				getFilterSearch().registerValues(column, stringStream.collect(Collectors.toSet()));
			}
			catch (Exception e) {
				log.error("Problem collecting column values for {}", column, e);
			}
		}
	}

	@Override
	public void close() {
		closeDslContextWrapper();
		super.close();
	}

	@Override
	public void remove() {
		closeDslContextWrapper();
		super.remove();
	}

	private void closeDslContextWrapper() {
		try {
			dslContextWrapper.close();
		}
		catch (IOException e) {
			log.warn("Could not  close namespace's {} DSLContext/Datasource directly", getDataset().getId(), e);
		}
	}

}
