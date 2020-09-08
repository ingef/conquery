package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ExecutionStatus.CreationFlag;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoredQueriesProcessor {

	@Getter
	private final Namespaces namespaces;
	private final MetaStorage storage;

	public StoredQueriesProcessor(Namespaces namespaces) {
		this.namespaces = namespaces;
		this.storage = namespaces.getMetaStorage();
	}

	public Stream<ExecutionStatus> getAllQueries(Dataset dataset, HttpServletRequest req, User user) {
		Collection<ManagedExecution<?>> allQueries = storage.getAllExecutions();

		return allQueries
			.stream()
			// to exclude subtypes from somewhere else
			.filter(q -> (q instanceof ManagedQuery) && ((ManagedQuery) q).getQuery().getClass().equals(ConceptQuery.class))
			.filter(q -> q.getDataset().equals(dataset.getId()))
			.filter(q -> user.isPermitted(QueryPermission.onInstance(Ability.READ, q.getId())))
			.flatMap(mq -> {
				try {
					return Stream.of(
						mq.buildStatus(
							storage,
							URLBuilder.fromRequest(req),
							user));
				}
				catch (Exception e) {
					log.warn("Could not build status of " + mq, e);
					return Stream.empty();
				}
			});
	}

	public void deleteQuery(Dataset dataset, ManagedExecution<?> query) {
		storage.removeExecution(query.getId());
	}

	public ExecutionStatus getQueryWithSource(Dataset dataset, ManagedExecutionId queryId, User user) {
		ManagedExecution<?> query = storage.getExecution(queryId);
		if (query == null) {
			return null;
		}
		return query.buildStatus(storage, null, user, EnumSet.of(CreationFlag.WITH_COLUMN_DESCIPTION, CreationFlag.WITH_SOURCE));
	}

	public void patchQuery(User user, ManagedExecutionId executionId, MetaDataPatch patch) throws JSONException {
		ManagedExecution<?> execution = Objects.requireNonNull(storage.getExecution(executionId), String.format("Could not find form config %s", executionId));
		log.trace("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), executionId, patch);
		patch.applyTo(execution, storage, user, QueryPermission::onInstance);
		storage.updateExecution(execution);
		
		// Patch this query in other datasets
		List<Dataset> remainingDatasets = namespaces.getAllDatasets(() -> new ArrayList<>());
		remainingDatasets.remove(namespaces.get(executionId.getDataset()).getDataset());
		for(Dataset dataset : remainingDatasets) {
			ManagedExecutionId id = new ManagedExecutionId(dataset.getId(),executionId.getExecution());
			execution = storage.getExecution(id);
			if(execution == null) {
				continue;
			}
			log.trace("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), id, patch);
			patch.applyTo(execution, storage, user, QueryPermission::onInstance);
			storage.updateExecution(execution);
		}
	}

}
