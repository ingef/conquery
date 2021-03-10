package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.buildDatasetAbilityMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.FullExecutionStatus;
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

@Slf4j
@RequiredArgsConstructor
public class StoredQueriesProcessor {

    @Getter
    private final DatasetRegistry datasetRegistry;
    private final MetaStorage storage;
    private final ConqueryConfig config;

    public Stream<ExecutionStatus> getAllQueries(Namespace namespace, HttpServletRequest req, User user) {
        Collection<ManagedExecution<?>> allQueries = storage.getAllExecutions();

        return getQueriesFiltered(namespace.getDataset().getId(), RequestAwareUriBuilder.fromRequest(req), user, allQueries);
    }

    public Stream<ExecutionStatus> getQueriesFiltered(DatasetId datasetId, UriBuilder uriBuilder, User user, Collection<ManagedExecution<?>> allQueries) {
        Map<DatasetId, Set<Ability>> datasetAbilities = buildDatasetAbilityMap(user, datasetRegistry);

        return allQueries
                .stream()
                // to exclude subtypes from somewhere else
                .filter(StoredQueriesProcessor::canFrontendRender)
                // The following only checks the dataset, under which the query was submitted, but a query can target more that
                // one dataset.
                .filter(q -> q.getDataset().equals(datasetId))
                .filter(q -> q.getState().equals(ExecutionState.DONE) || q.getState().equals(ExecutionState.NEW))
                // We decide, that if a user owns an execution it is permitted to see it, which saves us a lot of permissions
                // However, for other executions we check because those are probably shared.
                .filter(q -> q.getOwner().equals(user.getId()) || user.isPermitted(QueryPermission.onInstance(Ability.READ, q.getId())))
                .flatMap(mq -> {
                    try {
                        return Stream.of(
                                mq.buildStatusOverview(
                                        storage,
                                        uriBuilder,
                                        user,
                                        datasetRegistry,
                                        datasetAbilities));
                    } catch (Exception e) {
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

    public void deleteQuery(ManagedExecutionId executionId, User user) {
        ManagedExecution<?> execution = Objects.requireNonNull(storage.getExecution(executionId));

        storage.removeExecution(executionId);
    }

    public FullExecutionStatus getQueryFullStatus(ManagedExecutionId queryId, User user, UriBuilder url) {
        ManagedExecution<?> query = storage.getExecution(queryId);

        authorize(user, query, Ability.READ);

        if (query == null) {
            return null;
        }
        query.initExecutable(datasetRegistry, config);

        Map<DatasetId, Set<Ability>> datasetAbilities = buildDatasetAbilityMap(user, datasetRegistry);
        return query.buildStatusFull(storage, url, user, datasetRegistry, datasetAbilities);
    }

    public void patchQuery(User user, ManagedExecutionId executionId, MetaDataPatch patch) throws JSONException {
        ManagedExecution<?> execution = Objects.requireNonNull(storage.getExecution(executionId), String.format("Could not find form config %s", executionId));
        log.trace("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), executionId, patch);
        patch.applyTo(execution, storage, user, QueryPermission::onInstance);
        storage.updateExecution(execution);

        // Patch this query in other datasets
        List<Dataset> remainingDatasets = datasetRegistry.getAllDatasets(() -> new ArrayList<>());
        remainingDatasets.remove(datasetRegistry.get(executionId.getDataset()).getDataset());
        for (Dataset dataset : remainingDatasets) {
            ManagedExecutionId id = new ManagedExecutionId(dataset.getId(), executionId.getExecution());
            execution = storage.getExecution(id);
            if (execution == null) {
                continue;
            }
            log.trace("Patching {} ({}) with patch: {}", execution.getClass().getSimpleName(), id, patch);
            patch.applyTo(execution, storage, user, QueryPermission::onInstance);
            storage.updateExecution(execution);
        }
    }

}
