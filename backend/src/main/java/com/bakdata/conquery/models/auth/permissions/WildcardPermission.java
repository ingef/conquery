package com.bakdata.conquery.models.auth.permissions;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

/**
 * Needed for (de)serialization with Jackson.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@CPSType(id = "WILDCARD_PERMISSION", base = ConqueryPermission.class)
public class WildcardPermission extends org.apache.shiro.authz.permission.WildcardPermission implements ConqueryPermission {
	
	private final Instant creationTime;
	
	@JsonCreator
	public WildcardPermission(List<Set<String>> parts, Instant creationTime) {
		this.setParts(parts);
		// Optional for backward compatibility TODO remove
		if(creationTime == null) {
			this.creationTime = Instant.now();
			return;
		}
		this.creationTime = creationTime;
	}

	public WildcardPermission(String wildcardString){
		super(wildcardString);
		creationTime = Instant.now();
	}

	public List<Set<String>> getParts() {
		return super.getParts();
	}
}
