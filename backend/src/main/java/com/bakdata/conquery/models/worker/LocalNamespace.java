package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.local.SqlEntityResolver;
import com.bakdata.conquery.mode.local.SqlStorageHandler;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.sql.DSLContextWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class LocalNamespace extends Namespace {

	private final DSLContextWrapper dslContextWrapper;
	private final SqlStorageHandler storageHandler;

	public LocalNamespace(
			ObjectMapper preprocessMapper,
			NamespaceStorage storage,
			ExecutionManager executionManager,
			DSLContextWrapper dslContextWrapper,
			SqlStorageHandler storageHandler,
			JobManager jobManager,
			FilterSearch filterSearch,
			SqlEntityResolver sqlEntityResolver,
			List<Injectable> injectables
	) {
		super(preprocessMapper, storage, executionManager, jobManager, filterSearch, sqlEntityResolver, injectables);
		this.dslContextWrapper = dslContextWrapper;
		this.storageHandler = storageHandler;
	}

	@Override
	void updateMatchingStats() {
		// TODO Build basic statistic on data
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
