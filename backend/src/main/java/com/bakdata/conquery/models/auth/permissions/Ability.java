package com.bakdata.conquery.models.auth.permissions;

import java.util.Collections;
import java.util.Set;

public enum Ability {
	DELETE,
	READ,
	CREATE,

	// Query specific
	TAG,
	CANCEL,
	LABEL,
	SHARE,

	// Dataset specific
	DOWNLOAD,
	PRESERVE_ID,
	
	// FormConfig specific
	MODIFY
	;
	
	private final Set<Ability> asSet = Collections.singleton(this);
	
	public Set<Ability> asSet() {
		return asSet;
	}
}
