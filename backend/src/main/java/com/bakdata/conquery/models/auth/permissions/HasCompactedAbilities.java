package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

/**
 * Interface for permissions, that combine multiple abilities in one permission.
 * This encapsules the abilities from being accessed directly from outside.
 *
 */
public interface HasCompactedAbilities {
	void setAbilities(Set<Ability> accesses);
	boolean addAbilities(Set<Ability> accesses);
	Set<Ability> getAbilitiesCopy();
	Set<Ability> allowedAbilities();
	boolean removeAllAbilities(Set<Ability> delAbilities);
}
