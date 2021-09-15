package com.bakdata.conquery.models.auth.entities;

import java.util.Set;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

public interface RoleOwner {
	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoleOwner.class);

	void addRole(Role role);

	void removeRole(Role role);

	/**
	 * Return a copy of the roles hold by the owner.
	 *
	 * @return A set of the roles hold by the owner.
	 */
	//TODO migrate to NsIdRef Role
	@JsonIgnore
	Set<RoleId> getRoles();



	default Set<ConqueryPermission> collectRolePermissions(Set<ConqueryPermission> permissions, MetaStorage storage) {
		for (RoleId roleId : getRoles()) {
			Role role = storage.getRole(roleId);
			if (role == null) {
				log.warn("Could not resolve role id [{}]", roleId);
				continue;
			}
			permissions = Sets.union(permissions, role.getEffectivePermissions());
		}
		return permissions;
	}
}
