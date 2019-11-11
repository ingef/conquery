package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.bakdata.conquery.models.auth.permissions.WildcardPermission;

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
	
	public static FEPermission from(WildcardPermission cPermission) {
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
