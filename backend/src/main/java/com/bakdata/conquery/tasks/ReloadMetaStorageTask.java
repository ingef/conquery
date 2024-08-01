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
			final int allUsers = storage.getAllUsers().size();
			final int allExecutions = storage.getAllExecutions().size();
			final int allFormConfigs = storage.getAllFormConfigs().size();
			final int allGroups = storage.getAllGroups().size();
			final int allRoles = storage.getAllRoles().size();

			log.debug("BEFORE: Have {} Users, {} Groups, {} Roles, {} Executions, {} FormConfigs.",
					  allUsers, allGroups, allRoles, allExecutions, allFormConfigs);
		}

		storage.loadData();
		output.println("DONE reloading MetaStorage within %s.".formatted(timer.elapsed()));

		{
			final int allUsers = storage.getAllUsers().size();
			final int allExecutions = storage.getAllExecutions().size();
			final int allFormConfigs = storage.getAllFormConfigs().size();
			final int allGroups = storage.getAllGroups().size();
			final int allRoles = storage.getAllRoles().size();

			log.debug("AFTER: Have {} Users, {} Groups, {} Roles, {} Executions, {} FormConfigs.",
					  allUsers, allGroups, allRoles, allExecutions, allFormConfigs);
		}



	}
}
