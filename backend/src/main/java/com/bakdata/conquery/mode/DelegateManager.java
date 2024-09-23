package com.bakdata.conquery.mode;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.servlets.tasks.Task;
import lombok.Value;

/**
 * Generic manager that contains shared data.
 *
 * @param <N> type of the namespace
 */
@Value
public class DelegateManager<N extends Namespace> implements Manager {
	ConqueryConfig config;
	Environment environment;
	DatasetRegistry<N> datasetRegistry;
	MetaStorage storage;
	ImportHandler importHandler;
	StorageListener storageListener;
	Supplier<Collection<ShardNodeInformation>> nodeProvider;
	List<Task> adminTasks;
	InternalMapperFactory internalMapperFactory;
	JobManager jobManager;

	@Override
	public void stop() throws Exception {
		jobManager.close();
		datasetRegistry.close();
	}

	@Override
	public MetaStorage getMetaStorage() {
		return storage;
	}
}
