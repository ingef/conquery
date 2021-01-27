package com.bakdata.conquery.models.config;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.validation.Validator;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface StorageFactory {

	MetaStorage createMetaStorage(Validator validator, DatasetRegistry datasets);

	NamespaceStorage createNamespaceStorage(Validator validator, String directory, boolean returnNullOnExisting);

	WorkerStorage createWorkerStorage(Validator validator, String directory, boolean returnNullOnExisting);

	void loadNamespaceStorages(ManagerNode managerNode);

	void loadWorkerStorages(ShardNode shardNode);
}
