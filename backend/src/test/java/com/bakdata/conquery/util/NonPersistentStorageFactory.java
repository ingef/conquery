package com.bakdata.conquery.util;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.worker.DatasetRegistry;

import javax.validation.Validator;
import java.util.List;

public class NonPersistentStorageFactory implements StorageFactory {
    @Override
    public MetaStorage createMetaStorage(Validator validator, List<String> pathName, DatasetRegistry datasets) {
        return new NonPersistentMetaStorage(datasets);
    }

    @Override
    public NamespaceStorage createNamespaceStorage(Validator validator, List<String> pathName, boolean returnNullOnExisting) {
        return new NonPersistentNamespaceStorage(validator);
    }

    @Override
    public WorkerStorage createWorkerStorage(Validator validator, List<String> pathName, boolean returnNullOnExisting) {
        return null;
    }

    @Override
    public void loadNamespaceStorages(ManagerNode managerNode) {

    }

    @Override
    public void loadWorkerStorages(ShardNode shardNode) {

    }
}
