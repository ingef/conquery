package com.bakdata.conquery.models.execution;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.util.QueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for classes that are able to be patched with an {@link MetaDataPatch}.
 * Allows sharing of implementations among groups of a given user.
 */
public interface  Shareable extends Authorized {
	static final Logger log = LoggerFactory.getLogger(Shareable.class);

	/**
	 * Sets the flag that indicated if an instance is shared among groups.
	 */
	void setShared(boolean shared);

	default <ID extends Id<?>, S extends Identifiable<? extends ID> & Shareable & Authorized> Consumer<ShareInformation> sharer(MetaStorage storage, Subject subject) {
		if (!(this instanceof Identifiable<?>)) {
			log.warn("Cannot share {} ({}) because it does not implement Identifiable", this.getClass(), this.toString());
			return QueryUtils.getNoOpEntryPoint();
		}

		return (patch) -> {
			if (patch == null || patch.getGroups() == null) {
				return;
			}

			final S shareable = (S) this;
			// Collect groups that do not have access to this instance and remove their probable permission
			for (Group group : storage.getAllGroups()) {
				if (patch.getGroups().contains(group.getId())) {
					continue;
				}

				log.trace("User {} unshares instance {} ({}) from owner {}.", subject, shareable.getClass().getSimpleName(), shareable.getId(), group);

				group.removePermission(shareable.createPermission(AbilitySets.SHAREHOLDER));
			}


			// Resolve the provided groups
			final Set<Group> groups = patch.getGroups().stream().map(storage::getGroup).collect(Collectors.toSet());

			for(Group group : groups) {
				final ConqueryPermission sharePermission = shareable.createPermission(AbilitySets.SHAREHOLDER);
				group.addPermission(sharePermission);

				log.trace("User {} shares instance {} ({}). Adding permission {} to owner {}.", subject, shareable.getClass().getSimpleName(), shareable.getId(), sharePermission, group);
			}

			setShared(!patch.getGroups().isEmpty());
		};
		
	}

	public interface ShareInformation {
		List<GroupId> getGroups();
	}
}
