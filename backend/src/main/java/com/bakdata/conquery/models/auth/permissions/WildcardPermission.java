package com.bakdata.conquery.models.auth.permissions;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * A wrapper for the actual {@link org.apache.shiro.authz.permission.WildcardPermission}
 * class to use it in the {@link CPSType} system and enable (de)serialization with Jackson.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@CPSType(id = "WILDCARD_PERMISSION", base = ConqueryPermission.class)
public class WildcardPermission extends org.apache.shiro.authz.permission.WildcardPermission implements ConqueryPermission {

	private final Instant creationTime;

	/**
	 * Constructor used for deserialization.
	 *
	 * @param parts        String parts that hold the [domains (, abilities (, instances))].
	 * @param creationTime The creation time of the permission.
	 */
	@JsonCreator
	public WildcardPermission(List<Set<String>> parts, Instant creationTime) {
		this.setParts(parts);
		// Optional for backward compatibility
		// TODO remove later
		if (creationTime == null) {
			this.creationTime = Instant.now();
			return;
		}
		this.creationTime = creationTime;
	}

	/**
	 * Creates a permission from a String.
	 * The creation time is set automatically.
	 *
	 * @param wildcardString Permission representation as a string in the format of {@link org.apache.shiro.authz.permission.WildcardPermission}.
	 */
	public WildcardPermission(String wildcardString) {
		super(wildcardString);
		creationTime = Instant.now();
	}

	/**
	 * Proxies the protected super implementation
	 */
	public List<Set<String>> getParts() {
		return super.getParts();
	}

	@Override
	@JsonIgnore
	public Set<String> getDomains() {
		return getParts().get(0);
	}

	@Override
	@JsonIgnore
	public Set<String> getAbilities() {
		if (getParts().size() > 1) {
			return getParts().get(1);
		}
		return Collections.emptySet();
	}

	@Override
	@JsonIgnore
	public Set<String> getInstances() {
		if (getParts().size() > 2) {
			return getParts().get(2);
		}
		return Collections.emptySet();
	}
}
