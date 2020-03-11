package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.ExternalIdChecker;
import com.bakdata.conquery.util.QueryUtils.SingleReusedChecker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RequiredArgsConstructor
public class QueryProcessor {

	@Getter
	private final Namespaces namespaces;
	private final MasterMetaStorage storage;


	/**
	 * Creates a query for all datasets, then submits it for execution on the
	 * intended dataset.
	 */
	public ExecutionStatus postQuery(Dataset dataset, SubmittedQuery query, URLBuilder urlb, User user) {
		
		// Initialize checks that need to traverse the query tree
		ExternalIdChecker externalIdChecker = new QueryUtils.ExternalIdChecker();
		SingleReusedChecker singleReusedChecker = new QueryUtils.SingleReusedChecker();

		// Chain the checks and apply them to the tree
		query.visit(externalIdChecker.andThen(singleReusedChecker));

		// Evaluate the checks and take action
		{
			// If this is only a re-executing query, execute the underlying query instead.
			final ManagedExecutionId executionId = singleReusedChecker.getOnlyReused();

			if (executionId != null) {
				log.info("Re-executing Query {}", executionId);


				final ManagedExecution<?> mq = ExecutionManager.execute( namespaces, storage.getExecution(executionId));

				return getStatus(dataset, mq, urlb, user);
			}
			
			// Check if the query contains parts that require to resolve external ids. If so the user must have the preserve_id permission on the dataset.
			if(externalIdChecker.resolvesExternalIds()) {
				user.checkPermission(DatasetPermission.onInstance(Ability.PRESERVE_ID, dataset.getId()));
			}
		}
		
		// Run the query on behalf of the user
		ManagedExecution<?> mq = ExecutionManager.runQuery(namespaces, query, user.getId(), dataset.getId());
		
		// Set abilities for submitted query
		user.addPermission(storage, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, mq.getId()));

		if(query instanceof IQuery) {
			translateToOtherDatasets(dataset, query, user, mq);
		}

		// return status
		return getStatus(dataset, mq, urlb, user);
	}

	private void translateToOtherDatasets(Dataset dataset, SubmittedQuery query, User user, ManagedExecution<?> mq) {
		IQuery translateable = (IQuery) query;
		// translate the query for all other datasets of user and submit it.
		for (Namespace targetNamespace : namespaces.getNamespaces()) {
			if (!user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), targetNamespace.getDataset().getId()))
				|| targetNamespace.getDataset().equals(dataset)) {
				continue;
			}
			
			// Ensure that user is allowed to read all sub-queries of the actual query.
			
			if (!translateable.collectRequiredQueries().stream()
				.allMatch(qid -> user.isPermitted(QueryPermission.onInstance(Ability.READ.asSet(), qid)))) {
				continue;				
			}
			
			try {
				DatasetId targetDataset = targetNamespace.getDataset().getId();
				IQuery translated = QueryTranslator.replaceDataset(namespaces, translateable, targetDataset);
				final ManagedExecution<?> mqTranslated = ExecutionManager.createQuery(namespaces, translated, mq.getQueryId(), user.getId(), targetDataset);
				
				user.addPermission(storage, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, mqTranslated.getId()));
			}
			catch (Exception e) {
				log.trace("Could not translate " + query + " to dataset " + targetNamespace.getDataset(), e);
			}
		}
	}

	public ExecutionStatus getStatus(Dataset dataset, ManagedExecution<?> query, URLBuilder urlb, User user) {
		return query.buildStatus(storage, urlb, user);
	}

	public ExecutionStatus cancel(Dataset dataset, ManagedExecution<?> query, URLBuilder urlb) {
		// TODO implement query cancel functionality
		return null;
	}
}
