package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.worker.Worker;

public interface QueryPlanContext {

	Worker getWorker();

	BlockManager getBlockManager();

	CentralRegistry getCentralRegistry();

	QueryPlanContext withGenerateSpecialDateUnion(boolean b);
	
	boolean isGenerateSpecialDateUnion();
}
