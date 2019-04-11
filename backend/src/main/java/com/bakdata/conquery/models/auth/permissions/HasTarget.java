package com.bakdata.conquery.models.auth.permissions;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface HasTarget {
	@JsonIgnore
	Object getTarget();
}
