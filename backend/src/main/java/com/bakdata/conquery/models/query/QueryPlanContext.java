package com.bakdata.conquery.models.query;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Worker;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.time.LocalDate;

@RequiredArgsConstructor @AllArgsConstructor @Getter @With
public class QueryPlanContext {

	@Getter(AccessLevel.NONE)
	private final Worker worker;

	private boolean generateSpecialDateUnion = true;
	private Range<LocalDate> dateRestriction;

	public DatasetId getDataset() {
		return worker.getStorage().getDataset().getId();
	}

	public WorkerStorage getStorage() {
		return worker.getStorage();
	}

	public CentralRegistry getCentralRegistry() {
		return worker.getStorage().getCentralRegistry();
	}

	public BlockManager getBlockManager() {
		return worker.getStorage().getBlockManager();
	}
}
