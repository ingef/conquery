package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;

@CPSType(id = "QUERY", base = StringPermission.class)
public class QueryPermission extends StringPermission{

	private static final String DOMAIN = "queries";

	private static final Set<Ability> ALLOWED_ABILITIES = AbilitySets.QUERY_CREATOR;
	
	public final static QueryPermission INSTANCE = new QueryPermission();
	
	
	public PermissionMixin instancePermission(Ability ability, ManagedExecutionId instance) {
		return instancePermission(ability, instance.toString());
	}
	
	public PermissionMixin instancePermission(Set<Ability> abilities, ManagedExecutionId instance) {
		return instancePermission(abilities, instance.toString());
	}

	@Override
	public String getDomain() {
		return DOMAIN;
	}

	@Override
	public Set<Ability> getAllowedAbilities() {
		return ALLOWED_ABILITIES;
	}
}
