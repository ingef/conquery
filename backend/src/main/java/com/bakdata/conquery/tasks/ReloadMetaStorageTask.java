package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.google.common.base.Stopwatch;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReloadMetaStorageTask extends Task {

	private final MetaStorage storage;

	public ReloadMetaStorageTask(MetaStorage storage) {
		super("reload-meta-storage");
		this.storage = storage;
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		final Stopwatch timer = Stopwatch.createStarted();

		output.println("BEGIN reloading MetaStorage.");

		{
			final long allUsers = storage.getAllUsers().count();
			final long allExecutions = storage.getAllExecutions().count();
			final long allFormConfigs = storage.getAllFormConfigs().count();
			final long allGroups = storage.getAllGroups().count();
			final long allRoles = storage.getAllRoles().count();

			log.debug("BEFORE: Have {} Users, {} Groups, {} Roles, {} Executions, {} FormConfigs.",
					  allUsers, allGroups, allRoles, allExecutions, allFormConfigs);
		}

		output.println("DONE reloading MetaStorage within %s.".formatted(timer.elapsed()));

		{
			final long allUsers = storage.getAllUsers().count();
			final long allExecutions = storage.getAllExecutions().count();
			final long allFormConfigs = storage.getAllFormConfigs().count();
			final long allGroups = storage.getAllGroups().count();
			final long allRoles = storage.getAllRoles().count();

			log.debug("AFTER: Have {} Users, {} Groups, {} Roles, {} Executions, {} FormConfigs.",
					  allUsers, allGroups, allRoles, allExecutions, allFormConfigs);
		}



	}
}
