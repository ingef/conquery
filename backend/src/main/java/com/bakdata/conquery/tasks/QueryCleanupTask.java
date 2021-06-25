package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.util.QueryUtils;
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
	private static final Predicate<String>
			UUID_PATTERN =
			Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").asPredicate();

	private final MetaStorage storage;
	private Duration queryExpiration;

	public QueryCleanupTask(MetaStorage storage, Duration queryExpiration) {
		super("query-cleanup");
		this.storage = storage;
		this.queryExpiration = queryExpiration;
	}

	public static boolean isDefaultLabel(String label) {
		return UUID_PATTERN.test(label);
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {

		Duration queryExpiration = this.queryExpiration;

		if (parameters.containsKey(EXPIRATION_PARAM)) {
			if (parameters.get(EXPIRATION_PARAM).size() > 1) {
				log.warn("Will not respect more than one expiration time. Have `{}`", parameters.get(EXPIRATION_PARAM));
			}

			queryExpiration = Duration.parse(parameters.get(EXPIRATION_PARAM).get(0));
		}

		if (queryExpiration == null) {
			throw new IllegalArgumentException("Query Expiration may not be null");
		}

		log.info("Starting deletion of queries older than {} of {}", queryExpiration, storage.getAllExecutions().size());

		// Iterate for as long as no changes are needed (this is because queries can be referenced by other queries)
		while (true) {
			final QueryUtils.AllReusedFinder reusedChecker = new QueryUtils.AllReusedFinder();
			Set<ManagedExecution<?>> toDelete = new HashSet<>();

			for (ManagedExecution<?> execution : storage.getAllExecutions()) {

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
				log.trace("{} is not shared", execution.getId());


				if (ArrayUtils.isNotEmpty(execution.getTags())) {
					continue;
				}
				log.trace("{} has no tags", execution.getId());

				if (execution.getLabel() != null && !isDefaultLabel(execution.getLabel())) {
					continue;
				}
				log.trace("{} has no label", execution.getId());


				if (LocalDateTime.now().minus(queryExpiration).isBefore(execution.getCreationTime())) {
					continue;
				}
				log.trace("{} is not older than {}.", execution.getId(), queryExpiration);

				toDelete.add(execution);
			}

			// remove all queries referenced in reused queries.
			final Collection<ManagedExecution<?>> referenced =
					reusedChecker.getReusedElements().stream()
								 .map(CQReusedQuery::getQueryId)
								 .map(storage::getExecution)
								 .collect(Collectors.toSet());

			toDelete.removeAll(referenced);


			if (toDelete.isEmpty()) {
				log.info("No queries to delete");
				break;
			}

			log.info("Deleting {} Executions", toDelete.size());

			for (ManagedExecution<?> execution : toDelete) {
				log.trace("Deleting Execution[{}]", execution.getId());
				storage.removeExecution(execution.getId());
			}

		}
	}
}
