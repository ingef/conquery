package com.bakdata.conquery.models.auth.entities;

import java.util.Set;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface RoleOwner {

	void addRole(MetaStorage storage, Role role);

	void removeRole(MetaStorage storage, Role role);

	/**
	 * Return a copy of the roles hold by the owner.
	 *
	 * @return A set of the roles hold by the owner.
	 */
	@JsonIgnore
	Set<RoleId> getRoles();
}
