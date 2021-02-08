package com.bakdata.conquery.util;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import org.apache.commons.collections4.queue.UnmodifiableQueue;

import javax.validation.Validator;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@CPSType(id = "NON_PERSISTENT", base = StorageFactory.class)
public class NonPersistentStorageFactory implements StorageFactory {

    @Override
    public MetaStorage createMetaStorage(Validator validator, List<String> pathName, DatasetRegistry datasets) {
        return new NonPersistentMetaStorage(datasets, validator);
    }

    @Override
    public NamespaceStorage createNamespaceStorage(Validator validator, List<String> pathName) {
        return new NonPersistentNamespaceStorage(validator);
    }

    @Override
    public WorkerStorage createWorkerStorage(Validator validator, List<String> pathName) {
        return new NonPersistentWorkerStorage(validator);
    }

    @Override
    public Queue<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode, List<String> pathName) {

        return UnmodifiableQueue.unmodifiableQueue(new ArrayDeque<>());
    }

    @Override
    public Queue<WorkerStorage> loadWorkerStorages(ShardNode shardNode, List<String> pathName) {

        return UnmodifiableQueue.unmodifiableQueue(new ArrayDeque<>());
    }
}
