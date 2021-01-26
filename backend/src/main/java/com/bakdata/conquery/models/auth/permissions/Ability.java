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
	STATISTIC, // Not used yet: Only get aggregations over entities and statistics about query results
	DOWNLOAD,  // Allow download of per entity results
	PRESERVE_ID,  // Needs extra implementation: Allow the user to see the real id of entities and externally resolve real ids into conquery
	
	// FormConfig specific
	MODIFY
	;
	
	private final Set<Ability> asSet = Collections.singleton(this);
	
	public Set<Ability> asSet() {
		return asSet;
	}
}
