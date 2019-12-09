package com.bakdata.conquery.models.auth.permissions;

import java.util.Collections;
import java.util.Set;

@AbilityContainer
public enum Ability {
	DELETE,
	READ,
	CREATE,

	// Query Specific
	TAG,
	CANCEL,
	LABEL,
	SHARE,

	// Dataset Specific
	DOWNLOAD,
	PRESERVE_ID
	;
	
	private final Set<Ability> asSet = Collections.singleton(this);
	
	public Set<Ability> asSet() {
		return asSet;
	}
}
