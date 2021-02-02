package com.bakdata.conquery.apiv1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.SecondaryIdQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class StoredQueriesProcessor {

	@Getter
	private final DatasetRegistry datasetRegistry;
	private final MetaStorage storage;

	public Stream<ExecutionStatus> getAllQueries(Namespace namespace, HttpServletRequest req, User user) {
		Collection<ManagedExecution<?>> allQueries = storage.getAllExecutions();

		return getQueriesFiltered(namespace.getDataset().getId(), RequestAwareUriBuilder.fromRequest(req), user, allQueries);
	}

	public Stream<ExecutionStatus> getQueriesFiltered(DatasetId datasetId, UriBuilder uriBuilder, User user, Collection<ManagedExecution<?>> allQueries) {
		return allQueries
			.stream()
			// to exclude subtypes from somewhere else
			.filter(StoredQueriesProcessor::canFrontendRender)
			.filter(q -> q.getDataset().equals(datasetId))
			.filter(q -> q.getState().equals(ExecutionState.DONE) || q.getState().equals(ExecutionState.NEW))
			.filter(q -> user.isPermitted(QueryPermission.onInstance(Ability.READ, q.getId())))
			.flatMap(mq -> {
				try {
					return Stream.of(
						mq.buildStatusOverview(
							storage,
							uriBuilder,
							user,
							datasetRegistry));
				}
				catch (Exception e) {
					log.warn("Could not build status of " + mq, e);
					return Stream.empty();
				}
			});
	}

	private static boolean canFrontendRender(ManagedExecution<?> q) {
		if (!(q instanceof ManagedQuery)) {
			return false;
		}

		if (((ManagedQuery) q).getQuery().getClass().equals(ConceptQuery.class)) {
			return true;
		}

		if (((ManagedQuery) q).getQuery().getClass().equals(SecondaryIdQuery.class)) {
			return true;
		}

		return false;
	}

	public void deleteQuery(Namespace namespace, ManagedExecutionId queryId) {
		storage.removeExecution(queryId);
	}
	
	public ExecutionStatus getQueryFullStatus(ManagedExecutionId queryId, User user, UriBuilder url) {
		ManagedExecution<?> query = storage.getExecution(queryId);
		if (query == null) {
			return null;
		}
		return query.buildStatusFull(storage, url, user, datasetRegistry);
	}

	public void patchQuery(User user, ManagedExecutionId executionId, MetaDataPatch patch) throws JSONException {
		ManagedExecution<?> execution = Objects.requireNonNull(storage.getExecution(executionId), String.format("Could not find form config %s", executionId));
		log.trace("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), executionId, patch);
		patch.applyTo(execution, storage, user, QueryPermission::onInstance);
		storage.updateExecution(execution);
		
		// Patch this query in other datasets
		List<Dataset> remainingDatasets = datasetRegistry.getAllDatasets(() -> new ArrayList<>());
		remainingDatasets.remove(datasetRegistry.get(executionId.getDataset()).getDataset());
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
