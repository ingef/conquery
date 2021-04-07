package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

//TODO not the best name
public interface Authorized {
	ConqueryPermission createPermission(Set<Ability> abilities);
}
