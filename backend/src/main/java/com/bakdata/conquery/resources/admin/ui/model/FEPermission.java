package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.HasCompactedAbilities;
import com.bakdata.conquery.models.auth.permissions.HasTarget;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Frontend Permission -- special type that allows easier handling of permission in Freemarker. 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class FEPermission {

	private final String type;
	private final Set<Ability> abilities;
	private final Object target;

	public static FEPermission from(ConqueryPermission cPermission) {
		Set<Ability> abilities = null;
		if(cPermission instanceof HasCompactedAbilities) {
			abilities = ((HasCompactedAbilities)cPermission).copyAbilities();
		}
		Object target = null;
		if(cPermission instanceof HasTarget) {
			target = ((HasTarget) cPermission).getTarget();
		}
		return new FEPermission(
			cPermission.getClass().getAnnotation(CPSType.class).id(),
			abilities,
			target);
	}

}
