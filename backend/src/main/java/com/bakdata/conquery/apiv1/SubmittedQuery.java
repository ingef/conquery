package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface SubmittedQuery {
	
	/**
	 * Resolves the submitted query.
	 * @param storage
	 * @param namespaces
	 * @param userId
	 * @param submittedDataset
	 * @return
	 */
	ManagedExecution toManagedExecution(MasterMetaStorage storage, Namespaces namespaces, UserId userId, DatasetId submittedDataset);

}
