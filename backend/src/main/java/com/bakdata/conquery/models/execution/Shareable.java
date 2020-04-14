package com.bakdata.conquery.models.execution;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.MetaDataPatch.PermissionCreator;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.QueryUtils;
import lombok.NonNull;

public interface Shareable {
	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Shareable.class);
	
	boolean isShared();
	void setShared(boolean shared);
	
	
	default  <ID extends IId<?>,S extends Identifiable<? extends ID> & Shareable> Consumer<MetaDataPatch> sharer(
		MasterMetaStorage storage,
		User user,
		PermissionCreator<ID> sharedPermissionCreator) {
		if(!(this instanceof Identifiable<?>)) {
			log.warn("Cannot share {} ({}) because it does not implement Identifiable", this.getClass(), this.toString());
			return QueryUtils.getNoOpEntryPoint();
		}
		return (patch) -> {
			if(patch != null && patch.getShared()) {
				List<Group> groups;
				if(patch.getGroups() != null) {
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
		@NonNull MasterMetaStorage storage,
		@NonNull User user,
		@NonNull S shareable,
		@NonNull PermissionCreator<ID> sharedPermissionCreator,
		@NonNull O other,
		boolean shared) {
		
		ConqueryPermission sharePermission = sharedPermissionCreator.apply(AbilitySets.FORM_CONFIG_SHAREHOLDER, shareable.getId());
		if (shared) {
			other.addPermission(storage, sharePermission);
			log.trace("User {} shares query {}. Adding permission {} to group {}.", user, shareable, shareable.getId(), sharePermission, other);
		}
		else {
			other.removePermission(storage, sharePermission);
			log.trace("User {} unshares query {}. Removing permission {} from group {}.", user, shareable, shareable.getId(), sharePermission, other);
		}
		shareable.setShared(shared);
	}
}
