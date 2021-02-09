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
import java.util.Collection;
import java.util.List;
import java.util.Queue;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface StorageFactory {

	MetaStorage createMetaStorage(Validator validator, List<String> pathName, DatasetRegistry datasets);

	NamespaceStorage createNamespaceStorage(Validator validator, List<String> pathName);

	WorkerStorage createWorkerStorage(Validator validator, List<String> pathName);

	Collection<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode, List<String> pathName);

	Collection<WorkerStorage> loadWorkerStorages(ShardNode shardNode, List<String> pathName);
}
