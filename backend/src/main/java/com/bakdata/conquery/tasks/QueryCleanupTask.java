package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.util.QueryUtils;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Dropwizard Task deleting queries that are not used anymore. Defined as:
 * 	- not named
 * 	- older than 30 days
 * 	- is not shared
 * 	- no tags
 * 	- not referenced by other queries
 */
@Slf4j
public class QueryCleanupTask extends Task {

	MasterMetaStorage storage;

	public QueryCleanupTask(MasterMetaStorage storage) {
		super("query-cleanup");
		this.storage = storage;
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {

		// Iterate for as long as no changes are needed (this is because queries can be referenced by other queries)
		while (true) {
			final QueryUtils.AllReusedFinder reusedChecker = new QueryUtils.AllReusedFinder();
			List<ManagedExecutionId> toDelete = new ArrayList<>();

			for (ManagedExecution execution : storage.getAllExecutions()) {

				if (execution instanceof ManagedQuery) {
					((ManagedQuery) execution).getQuery().visit(reusedChecker);
				}

				if (execution.getLabel() != null)
					continue;

//				if (execution.getCreationTime().toLocalDate().isAfter(LocalDate.now().minusDays(30)))
//					continue;

				if (((ManagedQuery) execution).isShared())
					continue;

				if (ArrayUtils.isNotEmpty(((ManagedQuery) execution).getTags()))
					continue;

				toDelete.add(execution.getId());
			}

			toDelete.removeAll(reusedChecker.getReusedElements().stream().map(CQReusedQuery::getQuery).collect(Collectors.toList()));

			if(toDelete.isEmpty()) {
				break;
			}

			for (ManagedExecutionId managedExecutionId : toDelete) {
				log.debug("Deleting now unused Execution `{}`", managedExecutionId);
				storage.removeExecution(managedExecutionId);
			}
		}
	}
}
