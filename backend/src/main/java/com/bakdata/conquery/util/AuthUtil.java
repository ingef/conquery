package com.bakdata.conquery.util;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class AuthUtil {

	/**
	 * When the execution finishes, the service user and all its executions are cleaned up.
	 */
	public synchronized void cleanUpUserAndBelongings(User user, MetaStorage storage) {

		log.debug("BEGIN Removing user '{}' and it's assets", user);

		// Remove form configurations
		int countForms = 0;
		for (FormConfig formConfig : storage.getAllFormConfigs()) {
			if (!user.isOwner(formConfig)) {
				continue;
			}
			log.trace("Cleaning form config '{}' for user '{}'", formConfig.getId(), user.getId());
			storage.removeFormConfig(formConfig.getId());
			countForms++;
		}

		// Remove executions
		int countExecs = 0;
		for (ManagedExecution exec : storage.getAllExecutions()) {
			if (!user.isOwner(exec)) {
				continue;
			}
			log.trace("Cleaning execution '{}' for user '{}'", exec.getId(), user.getId());
			storage.removeExecution(exec.getId());
			countExecs++;
		}

		log.debug("Removed {} form configs and {} executions for user '{}'", countForms, countExecs, user);

		for (Group group : storage.getAllGroups()) {
			if (group.containsMember(user)) {
				group.removeMember(user);
				group.updateStorage();
				log.debug("Removed user '{}' from group '{}'", user.getId(), group.getId());
			}
		}

		storage.removeUser(user.getId());

		log.debug("FINISHED Removing user '{}' and it's assets", user.getId());
	}
}
