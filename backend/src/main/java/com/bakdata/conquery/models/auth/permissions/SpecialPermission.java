package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.google.common.collect.ImmutableSet;

import lombok.EqualsAndHashCode;
import lombok.Getter;


@EqualsAndHashCode(callSuper = true)
public abstract class SpecialPermission extends ConqueryPermission {
	
	public final static Set<Ability> ALLOWED_ABILITIES = ImmutableSet.of(Ability.DUMMY_ABILITY);
	@Getter
	private final String target;

	public SpecialPermission() {
		super(Ability.DUMMY_ABILITY.asSet());
		/**
		 * Generate unique dummy target for a SpecialPermission, since this is the only member that effects a different hash code
		 * for different types of SpecialPermissions.
		 */
		target = String.format("DUMMY_TARGET_%s", this.getClass().getAnnotation(CPSType.class).id());
	}


	@Override
	public Set<Ability> allowedAbilities() {
		return ALLOWED_ABILITIES;
	}
}
