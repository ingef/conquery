package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.local.SqlEntityResolver;
import com.bakdata.conquery.mode.local.SqlStorageHandler;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.sql.DSLContextWrapper;
import com.bakdata.conquery.sql.conquery.SqlMatchingStats;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class LocalNamespace extends Namespace {

	private final SqlDialect dialect;
	private final DSLContextWrapper dslContextWrapper;
	private final SqlStorageHandler storageHandler;
	private final SqlMatchingStats sqlMatchingStatsHandler;

	public LocalNamespace(
			SqlDialect dialect,
			ObjectMapper preprocessMapper,
			NamespaceStorage storage,
			ExecutionManager executionManager,
			DSLContextWrapper dslContextWrapper,
			SqlStorageHandler storageHandler,
			JobManager jobManager,
			SearchProcessor filterSearch,
			SqlEntityResolver sqlEntityResolver, SqlMatchingStats sqlMatchingStatsHandler
	) {
		super(preprocessMapper, storage, executionManager, jobManager, filterSearch, sqlEntityResolver);
		this.dslContextWrapper = dslContextWrapper;
		this.storageHandler = storageHandler;
		this.dialect = dialect;
		this.sqlMatchingStatsHandler = sqlMatchingStatsHandler;
	}

	@Override
	void updateMatchingStats() {
		getStorage().getAllConcepts()
				.filter(TreeConcept.class::isInstance)
					.forEach(concept -> sqlMatchingStatsHandler.createFunctionForConcept(((TreeConcept) concept), getDialect().getFunctionProvider(), getDslContextWrapper().getDslContext()));

		getStorage().getAllConcepts()
					.filter(TreeConcept.class::isInstance)
					.forEach(concept -> sqlMatchingStatsHandler.collectMatchingStatsForConcept(((TreeConcept) concept), getDialect().getFunctionProvider(), getDslContextWrapper().getDslContext()));



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

	private void closeDslContextWrapper() {
		try {
			dslContextWrapper.close();
		}
		catch (IOException e) {
			log.warn("Could not  close namespace's {} DSLContext/Datasource directly", getDataset().getId(), e);
		}
	}

	@Override
	public void remove() {
		closeDslContextWrapper();
		super.remove();
	}

}
