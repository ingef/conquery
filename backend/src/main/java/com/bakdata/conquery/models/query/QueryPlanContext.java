package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.events.SlaveBlockManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.worker.Worker;

public interface QueryPlanContext {

	Worker getWorker();
	
	SlaveBlockManager getBlockManager();

	CentralRegistry getCentralRegistry();
	
}
