package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

	private final Set<String> domains;
	private final Set<String> abilities;
	private final Set<String> targets;
	
	public static FEPermission from(WildcardPermission cPermission) {
		Set<String> domains = null;
		Set<String> abilities = null;
		Set<String> targets = null;
		List<Set<String>> parts = cPermission.getParts();
		switch (parts.size()) {
			// Using fall-through here to fill the object
			case 3:
				targets = parts.get(2);
			case 2:
				abilities = parts.get(1);
			case 1:
				domains = parts.get(0);
				break;
			default:
				throw new IllegalStateException(
					String.format("Permission %c has an unhandled number of parts: %d", cPermission, parts.size()));
		}
		return new FEPermission(
			domains,
			abilities,
			targets);
	}
	
	public static List<FEPermission> from(Collection<WildcardPermission> permissions) {
		return permissions.stream().map(FEPermission::from).collect(Collectors.toList());
	}

}
