package com.bakdata.conquery.models.query;

import java.util.Map;

import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.worker.Worker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class QueryPlanContextImpl implements QueryPlanContext {
	private final Worker worker;
	private final Map<ManagedQueryId, IQuery> dependencies;

	@Override
	public CentralRegistry getCentralRegistry() {
		return worker.getStorage().getCentralRegistry();
	}

	@Override
	public BlockManager getBlockManager() {
		return worker.getStorage().getBlockManager();
	}
}
