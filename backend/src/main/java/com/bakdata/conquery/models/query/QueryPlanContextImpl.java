package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.events.SlaveBlockManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.worker.Worker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class QueryPlanContextImpl implements QueryPlanContext {
	private final Worker worker;

	@Override
	public CentralRegistry getCentralRegistry() {
		return worker.getStorage().getCentralRegistry();
	}

	@Override
	public SlaveBlockManager getBlockManager() {
		return worker.getStorage().getBlockManager();
	}
}
