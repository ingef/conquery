package com.bakdata.conquery.models.auth.permissions;

import java.util.Collections;
import java.util.Set;

public enum Ability {
	DELETE,
	READ,
	
	// Query Specific
	TAG,
	CANCEL,
	LABEL,
	SHARE;
	
	private final Set<Ability> asSet = Collections.singleton(this);
	
	public Set<Ability> asSet() {
		return asSet;
	}
}
