package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import io.dropwizard.servlets.tasks.Task;
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

	public static final String EXPIRATION_PARAM = "expiration";
	public static final String DRY_RUN_PARAM = "dry-run";

	private final MetaStorage storage;
	private final Duration queryExpiration;

	public QueryCleanupTask(MetaStorage storage, Duration queryExpiration) {
		super("query-cleanup");
		this.storage = storage;
		this.queryExpiration = queryExpiration;
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {

		boolean dryRun = Optional.ofNullable(parameters.get(DRY_RUN_PARAM)).filter(Predicate.not(List::isEmpty)).map(List::getFirst).map(Boolean::parseBoolean).orElse(false);

		Duration queryExpiration = this.queryExpiration;

		if (parameters.containsKey(EXPIRATION_PARAM)) {
			if (parameters.get(EXPIRATION_PARAM).size() > 1) {
				log.warn("Will not respect more than one expiration time. Have `{}`", parameters.get(EXPIRATION_PARAM));
			}

			queryExpiration = Duration.parse(parameters.get(EXPIRATION_PARAM).getFirst());
		}

		if (queryExpiration == null) {
			throw new IllegalArgumentException("Query Expiration may not be null");
		}

		log.info("Starting deletion of queries older than {} of {}", queryExpiration, storage.getAllExecutions().count());

		// Iterate for as long as no changes are needed (this is because queries can be referenced by other queries)
		while (true) {
			final Set<ManagedExecutionId> requiredQueries = new HashSet<>();

			final Set<ManagedExecution> toDelete = new HashSet<>();

			for (ManagedExecution execution : storage.getAllExecutions().toList()) {

				// Gather all referenced queries via reused checker.
				requiredQueries.addAll(execution.getSubmitted().collectRequiredQueries());

				if (execution.isSystem()) {
					// System Queries will always be deleted.
					toDelete.add(execution);
					continue;
				}

				log.trace("{} is not system", execution.getId());

				if (execution.isShared()) {
					continue;
				}
				log.trace("{} is not shared", execution.getId());


				if (ArrayUtils.isNotEmpty(execution.getTags())) {
					continue;
				}
				log.trace("{} has no tags", execution.getId());

				if (execution.getLabel() != null && !execution.isAutoLabeled()) {
					continue;
				}
				log.trace("{} has no custom label", execution.getId());


				if (LocalDateTime.now().minus(queryExpiration).isBefore(execution.getCreationTime())) {
					continue;
				}
				log.trace("{} is not older than {}.", execution.getId(), queryExpiration);

				toDelete.add(execution);
			}

			// remove all queries referenced in reused queries.
			final Collection<ManagedExecution> referenced =
					requiredQueries.stream()
								   .map(storage::getExecution)
								   .collect(Collectors.toSet());

			toDelete.removeAll(referenced);


			if (toDelete.isEmpty()) {
				log.info("No queries to delete");
				break;
			}

			if (dryRun) {
			if (dryRun) {
				log.info("Dry Run: Cleanup would delete {} Executions", toDelete.size());
				return;
			}

log.info("Deleting {} Executions", toDelete.size());
				return;
			}

			log.info("Deleting {} Executions", toDelete.size());

			for (ManagedExecution execution : toDelete) {
				log.trace("Deleting Execution[{}]", execution.getId());
				storage.removeExecution(execution.getId());
			}

		}
	}
}
