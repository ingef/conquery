package com.bakdata.conquery.models.auth.permissions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Needed for (de)serialization with Jackson.
 *
 */
@SuppressWarnings("serial")
@Getter
@Setter
@CPSType(id = "WILDCARD_PERMISSION", base = ConqueryPermission.class)
public class WildcardPermission extends org.apache.shiro.authz.permission.WildcardPermission implements ConqueryPermission {
	
	private final Instant creationTime;
	
	@JsonCreator
	public WildcardPermission(SerializationContianer serCtx) {
		this.setParts(serCtx.getParts());
		// Optional for backward compatibility TODO remove
		creationTime = Optional.ofNullable(serCtx.getCreationTime()).orElse(Instant.now());	
	}

	public WildcardPermission(String wildcardString){
		super(wildcardString);
		creationTime = Instant.now();
	}

	public List<Set<String>> getParts() {
		return super.getParts();
	}
	
	@Getter @Setter
	public static class SerializationContianer {
		@NotEmpty
		private List<Set<String>> parts;
		// Commented for backward compatibility: @NotNull
		private Instant creationTime;
	}
}
