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
	
	public final Set<Ability> AS_SET = toSet();
	
	private Set<Ability> toSet() {
		return Collections.singleton(this);
	}
}
