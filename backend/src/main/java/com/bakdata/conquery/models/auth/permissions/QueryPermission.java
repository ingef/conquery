package com.bakdata.conquery.models.auth.permissions;

import java.util.EnumSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;

@CPSType(id = "QUERY", base = StringPermissionBuilder.class)
public class QueryPermission extends StringPermissionBuilder{

	private static final String DOMAIN = "queries";
	

	public final static EnumSet<Ability> ALLOWED_ABILITIES = EnumSet.of(
		Ability.READ,
		Ability.DELETE,
		Ability.SHARE,
		Ability.TAG,
		Ability.CANCEL,
		Ability.LABEL,
		Ability.DOWNLOAD
		);
	
	public final static QueryPermission INSTANCE = new QueryPermission();
	
	
	public ConqueryPermission instancePermission(Ability ability, ManagedExecutionId instance) {
		return instancePermission(ability, instance.toString());
	}
	
	public ConqueryPermission instancePermission(Set<Ability> abilities, ManagedExecutionId instance) {
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
