package com.bakdata.conquery.mode;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;

/**
 * A manager provides the implementations that differ by running mode.
 */
public interface Manager extends Managed {
	ConqueryConfig getConfig();
	Environment getEnvironment();
	DatasetRegistry<? extends Namespace> getDatasetRegistry();
	ImportHandler getImportHandler();
	StorageListener getStorageListener();
	Supplier<Collection<ShardNodeInformation>> getNodeProvider();
	List<Task> getAdminTasks();
	InternalObjectMapperCreator getInternalObjectMapperCreator();
	JobManager getJobManager();
	MetaStorage getStorage();
}
