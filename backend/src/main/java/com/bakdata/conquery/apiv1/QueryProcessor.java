package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class QueryProcessor {

	private final Namespaces namespaces;
	private final MasterMetaStorage storage;

	public ExecutionStatus postQuery(Dataset dataset, IQuery query, URLBuilder urlb, User user) throws JSONException {
		Namespace namespace = namespaces.get(dataset.getId());
		
		ManagedQuery mq = namespace.getQueryManager().createQuery(query, user);
		
		// Set abilities for submitted query
		user.addPermission(storage, new QueryPermission(AbilitySets.QUERY_CREATOR, mq.getId()));
		
		//translate the query for all other datasets of user
		namespacesLoop:
		for(Namespace ns : namespaces.getNamespaces()) {
			if(user.isPermitted(new DatasetPermission(Ability.READ.asSet(), ns.getDataset().getId()))
				&& !ns.getDataset().equals(dataset)) {
				
				try {
					IQuery translated = QueryTranslator.translate(namespaces, query, ns.getDataset().getId());
					
					for (ManagedExecutionId requiredQueryId : query.collectRequiredQueries()) {
						if(!user.isPermitted(new QueryPermission(Ability.READ.asSet(), requiredQueryId))) {
							continue namespacesLoop;
						}
					}
					
					ManagedQuery mTranslated = new ManagedQuery(translated, ns, user.getId());
					mTranslated.setQueryId(mq.getQueryId());
					namespace.getStorage().getMetaStorage().addExecution(mTranslated);
					user.addPermission(storage, new QueryPermission(AbilitySets.QUERY_CREATOR, mTranslated.getId()));
				}
				catch(Exception e) {
					log.trace("Could not translate "+query+" to dataset "+ns.getDataset(), e);
				}
			}
		}
		
		//return status
		return getStatus(dataset, mq, urlb);
	}

	public ExecutionStatus getStatus(Dataset dataset, ManagedExecution query, URLBuilder urlb) {
		return query.buildStatus(urlb);
	}

	public ExecutionStatus cancel(Dataset dataset, ManagedExecution query, URLBuilder urlb) {
		
		return null;
	}
}
