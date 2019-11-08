package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.HasCompactedAbilities;
import com.bakdata.conquery.models.auth.permissions.HasTarget;
import com.bakdata.conquery.models.auth.permissions.WildcardPermissionWrapper;

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
	private final Set<String> abilities;
	private final Set<String> target;

	public static FEPermission from(ConqueryPermission cPermission) {
		Set<String> abilities = null;
		if(cPermission instanceof HasCompactedAbilities) {
			abilities = ((HasCompactedAbilities)cPermission).getAbilitiesCopy().stream().map(String::valueOf).collect(Collectors.toSet());
		}
		Set<String> targets = null;
		if(cPermission instanceof HasTarget) {
			targets = Set.of(String.valueOf(((HasTarget) cPermission).getTarget()));
		}
		return new FEPermission(
			cPermission.getClass().getAnnotation(CPSType.class).id(),
			abilities,
			targets);
	}
	
	public static FEPermission from(WildcardPermissionWrapper cPermission) {
		String type = null;
		Set<String> abilities = null;
		Set<String> target = null;
		List<Set<String>> parts = cPermission.getParts();
		Iterator<Set<String>> it = parts.iterator();
		try {
			type = it.next().iterator().next();
			abilities = it.next();
			target = it.next();
		} catch(NoSuchElementException e) {
			// Do nothing because the permission might be a domain or ability-on-domain permission
		}
		return new FEPermission(
			type,
			abilities,
			target);
	}

}
