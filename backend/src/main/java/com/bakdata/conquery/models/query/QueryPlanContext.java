package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Worker;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

@RequiredArgsConstructor @AllArgsConstructor @Getter @With
public class QueryPlanContext {

	@Getter(AccessLevel.NONE)
	private final Worker worker;

	private boolean generateSpecialDateUnion = true;
	private CDateRange dateRestriction = CDateRange.all();

	public DatasetId getDataset() {
		return worker.getStorage().getDataset().getId();
	}

	public WorkerStorage getStorage() {
		return worker.getStorage();
	}

	public CentralRegistry getCentralRegistry() {
		return worker.getStorage().getCentralRegistry();
	}

	public BucketManager getBlockManager() {
		return worker.getStorage().getBlockManager();
	}
}
