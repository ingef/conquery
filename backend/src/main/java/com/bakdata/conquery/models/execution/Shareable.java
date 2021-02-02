package com.bakdata.conquery.models.execution;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.MetaDataPatch.PermissionCreator;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.util.QueryUtils;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for classes that are able to be patched with an {@link MetaDataPatch}.
 * Allows sharing of implementations among groups of a given user.
 */
public interface  Shareable {
	static final Logger log = LoggerFactory.getLogger(Shareable.class);
	
	/**
	 * Sets the flag that indicated if an instance is shared among groups.
	 */
	void setShared(boolean shared);
	
	
	default  <ID extends IId<?>,S extends Identifiable<? extends ID> & Shareable> Consumer<ShareInformation> sharer(
		MetaStorage storage,
		User user,
		PermissionCreator<ID> sharedPermissionCreator) {
		if(!(this instanceof Identifiable<?>)) {
			log.warn("Cannot share {} ({}) because it does not implement Identifiable", this.getClass(), this.toString());
			return QueryUtils.getNoOpEntryPoint();
		}
		return (patch) -> {
			if(patch != null && patch.getGroups() != null) {
				S shareable = (S) this;
				// Collect groups that do not have access to this instance and remove their probable permission
				storage.getAllGroups().stream()
						.filter(group -> !patch.getGroups().contains(group.getId()))
						.peek(group -> log.trace("User {} unshares instance {} ({}) from owner {}.", user, shareable.getClass().getSimpleName(), shareable.getId(), group))
						.forEach(group -> group.removePermission(storage, sharedPermissionCreator.apply(AbilitySets.SHAREHOLDER, shareable.getId())));


				if(!patch.getGroups().isEmpty()) {
					// Resolve the provided groups
					List<Group> groups = patch.getGroups().stream().map(id -> storage.getGroup(id)).collect(Collectors.toList());
					for(Group group : groups) {
						ConqueryPermission sharePermission = sharedPermissionCreator.apply(AbilitySets.SHAREHOLDER, shareable.getId());
						group.addPermission(storage, sharePermission);
						log.trace("User {} shares instance {} ({}). Adding permission {} to owner {}.", user, shareable.getClass().getSimpleName(), shareable.getId(), sharePermission, group);
					}
				}

				this.setShared(!patch.getGroups().isEmpty());
			}
		};
		
	}

	public interface ShareInformation {
		List<GroupId> getGroups();
	}
}
