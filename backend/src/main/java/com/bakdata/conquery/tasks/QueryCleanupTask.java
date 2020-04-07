package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.util.QueryUtils;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Dropwizard Task deleting queries that are not used anymore. Defined as:
 * - not named
 * - older than 30 days
 * - is not shared
 * - no tags
 * - not referenced by other queries
 */
@Slf4j
public class QueryCleanupTask extends Task {

	private final MasterMetaStorage storage;
	private Duration oldQueriesDuration;

	public QueryCleanupTask(MasterMetaStorage storage, Duration oldQueriesTime) {
		super("cleanup");
		this.storage = storage;
		this.oldQueriesDuration = oldQueriesTime;
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		log.info("Starting Cleanup Task.");

		// Iterate for as long as no changes are needed (this is because queries can be referenced by other queries)
		while (true) {
			final QueryUtils.AllReusedFinder reusedChecker = new QueryUtils.AllReusedFinder();
			List<ManagedExecutionId> toDelete = new ArrayList<>();

			final Collection<ManagedExecution<?>> allExecutions = storage.getAllExecutions();
			log.info("Found {} executions in storage.", allExecutions.size());

			for (ManagedExecution<?> execution : allExecutions) {

				// Gather all referenced queries via reused checker.
				if (execution instanceof ManagedQuery) {
					((ManagedQuery) execution).getQuery().visit(reusedChecker);
				}
				else if (execution instanceof ManagedForm) {
					((ManagedForm) execution).getFlatSubQueries().values()
											 .forEach(q -> q.getQuery().visit(reusedChecker));
				}

				if (execution.isShared()) {
					continue;
				}
				log.debug("{} is not shared", execution.getId());


				if (ArrayUtils.isNotEmpty(execution.getTags())) {
					continue;
				}
				log.debug("{} has no tags", execution.getId());

				if (execution.getLabel() != null) {
					continue;
				}else {
					log.debug("{} has no label", execution.getId());
				}


				if(execution.getCreationTime().until(LocalDateTime.now(), oldQueriesDuration.getUnit().toChronoUnit()) <= oldQueriesDuration.getQuantity()) {
					continue;
				}
				else {
					log.debug("{} is not older than {}.", execution.getId(), oldQueriesDuration);
				}

				toDelete.add(execution.getId());
			}

			// remove all queries referenced in reused queries.
			for (CQReusedQuery reusedElement : reusedChecker.getReusedElements()) {
				if (toDelete.remove(reusedElement.getQuery())) {
					log.debug("{} is reused", reusedElement.getQuery());
				}
			}


			if (toDelete.isEmpty()) {
				break;
			}

			for (ManagedExecutionId managedExecutionId : toDelete) {
				log.info("Deleting Execution[{}]", managedExecutionId);
				storage.removeExecution(managedExecutionId);
			}
		}
	}
}
