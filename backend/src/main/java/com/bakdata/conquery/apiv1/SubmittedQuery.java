package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeReadDatasets;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface SubmittedQuery extends Visitable {

	/**
	 * Transforms the submitted query to an {@link ManagedExecution}.
	 * In this step some external dependencies are resolve (such as {@link CQExternal}).
	 * However steps that require add or manipulates queries programmatically based on the submitted query
	 * should be done in an extra init procedure (see {@link ManagedForm#initExecutable(Namespaces)}.
	 * These steps are executed right before the execution of the query and not necessary in this creation phase.
	 * 
	 * @param storage Needed by {@link ManagedExecution} for the self update upon completion.
	 * @param namespaces
	 * @param userId
	 * @param submittedDataset
	 * @return
	 */
	ManagedExecution<?> toManagedExecution(Namespaces namespaces, UserId userId, DatasetId submittedDataset);

	
	Set<ManagedExecutionId> collectRequiredQueries();

	/**
	 * Check implementation specific permissions.
	 */
	default void checkPermissions(@NonNull User user) {
		// Also look into the query and check the datasets
		authorizeReadDatasets(user, this);
		// Check reused query
		for (ManagedExecutionId requiredQueryId : collectRequiredQueries()) {
			authorize(user, requiredQueryId, Ability.READ);
		}
	}
}
