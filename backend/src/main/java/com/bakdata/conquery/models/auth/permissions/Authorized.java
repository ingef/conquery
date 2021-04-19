package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

/**
 * Classes implementing this can require authorization for access. They will provide the necessary {@link ConqueryPermission} themselves.
 */
public interface Authorized {
	/**
	 * Create a Permission for this object with the requested abilities.
	 */
	ConqueryPermission createPermission(Set<Ability> abilities);
}
