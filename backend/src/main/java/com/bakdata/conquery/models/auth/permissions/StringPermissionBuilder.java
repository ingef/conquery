package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSBase;

/**
 * Base class with utility functions to build permissions.
 * Subclasses should wrap their call to the {@code instancePermission} functions with their own string representation.
 *
 */
@CPSBase
public abstract class StringPermissionBuilder {
	
	private static final String PART_DIVIDER_TOKEN = ":";
	private static final String SUBPART_DIVIDER_TOKEN = ",";
	private final static String DOMAIN_FORMAT	= "%s";
	private final static String ABILITY_FORMAT	= "%s"+PART_DIVIDER_TOKEN+"%s";
	private final static String INSTANCE_FORMAT	= "%s"+PART_DIVIDER_TOKEN+"%s"+PART_DIVIDER_TOKEN+"%s";

    //// DOMAIN ////
    public WildcardPermission domainPermission() {
		return new WildcardPermission(String.format(DOMAIN_FORMAT, getDomain()));
	}
	
    //// ABILITY on DOMAIN ////
	public WildcardPermission abilityPermission(Ability ability) {
		checkAbility(getAllowedAbilities(),ability);
		return new WildcardPermission(String.format(ABILITY_FORMAT, getDomain(), ability));
	}

	public WildcardPermission abilityPermission(Set<Ability> abilities) {
		checkAbilities(getAllowedAbilities(),abilities);
		return new WildcardPermission(String.format(ABILITY_FORMAT, getDomain(), abilities.stream().map(Ability::toString).collect(Collectors.joining(SUBPART_DIVIDER_TOKEN))));
	}

	//// ABILITY on INSTANCE in DOMAIN ////
	protected WildcardPermission instancePermission(Ability ability, String instance) {
		checkAbility(getAllowedAbilities(),ability);
		return new WildcardPermission(String.format(INSTANCE_FORMAT, getDomain(), ability, instance));
	}

	protected WildcardPermission instancePermission(Set<Ability> abilities, String instance) {
		checkAbilities(getAllowedAbilities(),abilities);
		return new WildcardPermission(String.format(INSTANCE_FORMAT, getDomain(), abilities.stream().map(Ability::toString).collect(Collectors.joining(SUBPART_DIVIDER_TOKEN)) , instance));
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
	
	/**
	 * The domain a permission grants access on (e.g. datasets).
	 * The domain name must not include PART_DIVIDER_TOKEN or the SUBPART_DIVIDER_TOKEN.
	 * @return The domain name.
	 */
	public abstract String getDomain();
	
	
	/**
	 * The Abilities, that are allowed for the domain.
	 * @return A set of the allowed abilities.
	 */
	public abstract Set<Ability> getAllowedAbilities();

}
