package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * For granting access to the admin servlet.
 *
 */
@Slf4j
@ToString(callSuper = true)
@CPSType(id = "ADMIN", base = StringPermission.class)
public final class AdminPermission extends StringPermission {


	private static final String DOMAIN = "admin";
	
	public final static AdminPermission INSTANCE = new AdminPermission();

	@Override
	public String getDomain() {
		return DOMAIN;
	}

	@Override
	public Set<Ability> getAllowedAbilities() {
		return Set.of();
	}
}
