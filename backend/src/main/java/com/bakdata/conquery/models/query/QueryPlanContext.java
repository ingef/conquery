package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.worker.Worker;

public interface QueryPlanContext {

	Worker getWorker();

	BucketManager getBlockManager();

	CentralRegistry getCentralRegistry();

	QueryPlanContext withGenerateSpecialDateUnion(boolean b);
	
	boolean isGenerateSpecialDateUnion();
}
