package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeReadDatasets;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NonNull;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface SubmittedQuery extends Visitable {
	
	/**
	 * Resolves the submitted query.
	 * @param storage
	 * @param namespaces
	 * @param userId
	 * @param submittedDataset
	 * @return
	 */
	ManagedExecution<?> toManagedExecution(MasterMetaStorage storage, Namespaces namespaces, UserId userId, DatasetId submittedDataset);


	Set<ManagedExecutionId> collectRequiredQueries();
	
	/**
	 * Check implementation specific permissions.
	 */
	default void checkPermissions(@NonNull User user){
		// Also look into the query and check the datasets
		authorizeReadDatasets(user, this);
		// Check reused query
		for (ManagedExecutionId requiredQueryId : collectRequiredQueries()) {
			authorize(user, requiredQueryId, Ability.READ);
		}
	}
}
