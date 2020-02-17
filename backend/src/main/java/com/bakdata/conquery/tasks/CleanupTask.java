package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.util.QueryUtils;
import com.google.common.collect.ImmutableMultimap;
import groovy.transform.SelfType;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CleanupTask extends Task {
	private  final MasterMetaStorage storage;

	/**
	 * Create a new task with the given name.
	 *
	 * @param storage the Storage to manage
	 */
	public CleanupTask(MasterMetaStorage storage) {
		super("cleanup");
		this.storage = storage;
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		List<ManagedExecutionId> delete = new ArrayList<>();
		Set<ManagedExecutionId> referenced = new HashSet<>();

		// Loop until no more queries are deleted, iteration is necessary because some queries can be referenced by others queries that might get deleted.
		do {
			delete.clear();
			referenced.clear();

			// Get all executions in descending order.
			final List<ManagedExecution> executions =
					storage.getAllExecutions().stream()
						   .sorted(Comparator.comparing(ManagedExecution::getCreationTime))
						   .collect(Collectors.toList());


			// First gather all
			for (ManagedExecution execution : executions) {
				if (execution.getLabel() != null
//					|| LocalDate.now().until(execution.getStartTime().toLocalDate()).getDays() < 30
					|| (execution instanceof ManagedQuery && ((ManagedQuery) execution).isShared())
					|| referenced.contains(execution.getId())
				) {
					// If this is a query referencing other queries, we may not delete it unless it is itself to be deleted etc.
					if (execution instanceof ManagedQuery) {
						final QueryUtils.AllReusedFinder allReusedFinder = new QueryUtils.AllReusedFinder();

						((ManagedQuery) execution).getQuery().visit(allReusedFinder);
						referenced.addAll(allReusedFinder.getReusedElements());
					}

					continue;
				}

				delete.add(execution.getId());
			}

			for (ManagedExecutionId executionId : delete) {
				log.debug("Deleting Query[{}]", executionId);
				storage.removeExecution(executionId);
			}

		} while (!delete.isEmpty());
	}
}
