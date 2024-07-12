package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.local.SqlStorageHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.sql.DSLContextWrapper;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Getter
@Slf4j
public class LocalNamespace extends Namespace {

	private final ConqueryConfig config;
	private final SqlExecutionService executionService;
	private final DSLContextWrapper dslContextWrapper;
	private final SqlStorageHandler storageHandler;

	public LocalNamespace(
			ConqueryConfig config,
			ObjectMapper preprocessMapper,
			ObjectMapper communicationMapper,
			NamespaceStorage storage,
			SqlExecutionService executionService,
			ExecutionManager<SqlExecutionResult> executionManager,
			DSLContextWrapper dslContextWrapper,
			SqlStorageHandler storageHandler,
			JobManager jobManager,
			FilterSearch filterSearch,
			IndexService indexService,
			List<Injectable> injectables
	) {
		super(preprocessMapper, communicationMapper, storage, executionManager, jobManager, filterSearch, indexService, injectables);
		this.config = config;
		this.executionService = executionService;
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
			}catch (Exception e) {
				log.error("Problem collecting column values for {}", column, e);
			}
		}
	}

	@Override
	String tryInnerResolveId(final EntityIdMap mapping, final EntityIdMap.ExternalId externalId) {

		Field<Object> primaryColumn = DSL.field(DSL.name(config.getIdColumns().findPrimaryIdColumn().getField()));
		Table<Record> fromTable = DSL.table(DSL.name(config.getIdColumns().getTable()));

		DSLContext context = dslContextWrapper.getDslContext();
		Select<?> idLookupQuery = context.select(primaryColumn)
										 .from(fromTable)
										 .where(primaryColumn.eq(externalId.getId()));

		Result<?> result = executionService.fetch(idLookupQuery);
		// empty result -> ID was not resolved
		return result.isEmpty() ? null : externalId.getId();
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
