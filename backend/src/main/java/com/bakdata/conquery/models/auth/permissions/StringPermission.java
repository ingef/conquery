package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSBase;

@CPSBase
public abstract class StringPermission {
	
	final static String DOMAIN_FORMAT = "%s";
	final static String ABILITY_FORMAT = "%s:%s";
	final static String INSTANCE_FORMAT = "%s:%s:%s";
    static final String SUBPART_DIVIDER_TOKEN = ",";

    //// DOMAIN ////
    public WildcardPermissionWrapper domainPermission() {
		return new WildcardPermissionWrapper(String.format(DOMAIN_FORMAT, getDomain()));
	}
	
    //// ABILITY on DOMAIN ////
	public WildcardPermissionWrapper abilityPermission(Ability ability) {
		checkAbility(getAllowedAbilities(),ability);
		return new WildcardPermissionWrapper(String.format(ABILITY_FORMAT, getDomain(), ability));
	}

	public WildcardPermissionWrapper abilityPermission(Set<Ability> abilities) {
		checkAbilities(getAllowedAbilities(),abilities);
		return new WildcardPermissionWrapper(String.format(ABILITY_FORMAT, getDomain(), abilities.stream().map(Ability::toString).collect(Collectors.joining(SUBPART_DIVIDER_TOKEN))));
	}

	//// ABILITY on INSTANCE in DOMAIN ////
	protected WildcardPermissionWrapper instancePermission(Ability ability, String instance) {
		checkAbility(getAllowedAbilities(),ability);
		return new WildcardPermissionWrapper(String.format(INSTANCE_FORMAT, getDomain(), ability, instance));
	}

	protected WildcardPermissionWrapper instancePermission(Set<Ability> abilities, String instance) {
		checkAbilities(getAllowedAbilities(),abilities);
		return new WildcardPermissionWrapper(String.format(INSTANCE_FORMAT, getDomain(), abilities.stream().map(Ability::toString).collect(Collectors.joining(SUBPART_DIVIDER_TOKEN)) , instance));
	}

	//// UTIL ////
	private static void checkAbility(Set<Ability> allowedAbilities, Ability ability) {
		if(!allowedAbilities.contains(ability)) {
			throw new IllegalArgumentException(String.format("Ability %s is not allowed. Allowed abilities:", ability, allowedAbilities));
		}
	}
	
	private static void checkAbilities(Set<Ability> allowedAbilities, Set<Ability>  abilities) {
		if(!allowedAbilities.containsAll(abilities)) {
			throw new IllegalArgumentException(String.format("Abilities %s are not allowed. Allowed abilities:", abilities, allowedAbilities));
		}
	}
	
	public abstract String getDomain();
	
	public abstract Set<Ability> getAllowedAbilities();

}
