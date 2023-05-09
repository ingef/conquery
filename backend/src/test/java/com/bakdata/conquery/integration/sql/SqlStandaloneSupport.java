package com.bakdata.conquery.integration.sql;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import lombok.Value;

@Value
public class SqlStandaloneSupport {

    Dataset dataset;
    NamespacedStorage storage;

    public SqlStandaloneSupport(Dataset dataset) {
        this.dataset = dataset;
        this.storage = new NamespacedStorage(new NonPersistentStoreFactory(), "", null) {};
        storage.openStores(Jackson.MAPPER);
        storage.updateDataset(dataset);
    }

}
