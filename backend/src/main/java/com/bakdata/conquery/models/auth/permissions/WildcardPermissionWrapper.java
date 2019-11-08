package com.bakdata.conquery.models.auth.permissions;

import java.util.List;
import java.util.Set;

import org.apache.shiro.authz.permission.WildcardPermission;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Needed for (de)serialization with Jackson.
 *
 */
@SuppressWarnings("serial")
@Getter
@Setter
@CPSType(id = "WILDCARD_PERMISSION", base = PermissionMixin.class)
public class WildcardPermissionWrapper extends WildcardPermission implements PermissionMixin{
	
	@JsonCreator
	public WildcardPermissionWrapper(List<Set<String>> parts) {
		this.setParts(parts);
	}
	
	@JsonCreator
	public WildcardPermissionWrapper(String wildcardString){
		super(wildcardString);
	}

	public List<Set<String>> getParts() {
		return super.getParts();
	}
}
