package com.bakdata.conquery.models.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.MetaDataPatch.PermissionCreator;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
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
			if(patch != null && patch.getShared() != null) {
				List<Group> groups;
				if(patch.getGroups() != null && !patch.getGroups().isEmpty()) {
					// Resolve the provided groups
					groups = patch.getGroups().stream().map(id -> storage.getGroup(id)).collect(Collectors.toList());
				}
				else {
					// If no groups are provided by the instance is to be shared, share it with all groups the user is member of
					groups = AuthorizationHelper.getGroupsOf(user, storage);
				}
				for(Group group : groups) {
					shareWithOther(
						storage,
						user,
						(S) this,
						sharedPermissionCreator, 
						group,
						patch.getShared());
				}
			}
		};
		
	}

	/**
	 * (Un)Shares a query with a specific group. Set or unsets the shared flag.
	 * Does persist this change made to the {@link Shareable}. 
	 */
	public static <ID extends IId<?>, S extends Identifiable<? extends ID> & Shareable, O extends PermissionOwner<? extends IId<O>>> void shareWithOther(
		@NonNull MetaStorage storage,
		@NonNull User user,
		@NonNull S shareable,
		@NonNull PermissionCreator<ID> sharedPermissionCreator,
		@NonNull O other,
		boolean shared) {
		
		ConqueryPermission sharePermission = sharedPermissionCreator.apply(AbilitySets.SHAREHOLDER, shareable.getId());
		if (shared) {
			other.addPermission(storage, sharePermission);
			log.trace("User {} shares instance {} ({}). Adding permission {} to owner {}.", user, shareable.getClass().getSimpleName(), shareable.getId(), sharePermission, other);
			shareable.setShared(shared);
		}
		else {
			other.removePermission(storage, sharePermission);
			log.trace("User {} unshares instance {} ({}). Removing permission {} from owner {}.", user, shareable.getClass().getSimpleName(), shareable.getId(), sharePermission, other);
			// Update shared flag
			boolean stillShared = false;
			List<GroupId> stillSharedGroups = new ArrayList<>();
			for (Group group : storage.getAllGroups()) {
				if (group.getPermissions().contains(sharePermission)) {
					stillShared = true;
					stillSharedGroups.add(group.getId());
				}
			}
			
			if(stillShared) {
				log.trace("After removing a share from instance {} ({}) it is still shared with the following groups: {}", shareable.getClass().getSimpleName(), shareable.getId(), stillSharedGroups);
			}
			else {
				log.trace("After removing a share from instance {} ({}) it is not shared anymore", shareable.getClass().getSimpleName(), shareable.getId());
			}
			
			shareable.setShared(stillShared);
		}
	}
	
	public interface ShareInformation {
		Boolean getShared();
		List<GroupId> getGroups();
	}
}
