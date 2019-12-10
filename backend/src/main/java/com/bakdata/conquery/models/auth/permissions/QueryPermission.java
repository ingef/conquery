package com.bakdata.conquery.models.auth.permissions;

import java.util.EnumSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;

@CPSType(id = "QUERY", base = StringPermissionBuilder.class)
public class QueryPermission extends StringPermissionBuilder{

	public static final String DOMAIN = "queries";
	

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
	
	
	private ConqueryPermission instancePermission(Ability ability, ManagedExecutionId instance) {
		return instancePermission(ability, instance.toString());
	}
	
	private ConqueryPermission instancePermission(Set<Ability> abilities, ManagedExecutionId instance) {
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
	
	
	//// Helper functions
	public static ConqueryPermission onInstance(Ability ability, ManagedExecutionId instance) {
		return INSTANCE.instancePermission(ability, instance);
	}
	
	public static ConqueryPermission onInstance(Set<Ability> abilities, ManagedExecutionId instance) {
		return INSTANCE.instancePermission(abilities, instance);
	}
}
