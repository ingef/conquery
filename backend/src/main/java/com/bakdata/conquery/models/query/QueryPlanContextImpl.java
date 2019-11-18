package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.worker.Worker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

@RequiredArgsConstructor @AllArgsConstructor @Getter @Wither
public class QueryPlanContextImpl implements QueryPlanContext {
	private final Worker worker;
	private boolean generateSpecialDateUnion = true;

	@Override
	public CentralRegistry getCentralRegistry() {
		return worker.getStorage().getCentralRegistry();
	}

	@Override
	public BucketManager getBlockManager() {
		return worker.getStorage().getBucketManager();
	}
}
