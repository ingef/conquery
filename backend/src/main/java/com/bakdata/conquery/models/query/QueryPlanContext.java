package com.bakdata.conquery.models.query;

import java.util.Map;

import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.worker.Worker;

public interface QueryPlanContext {

	Worker getWorker();
	
	Map<ManagedQueryId, IQuery> getDependencies();

	BlockManager getBlockManager();

	CentralRegistry getCentralRegistry();
	
}
