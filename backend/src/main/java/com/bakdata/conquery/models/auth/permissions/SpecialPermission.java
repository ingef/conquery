package com.bakdata.conquery.models.auth.permissions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
public abstract class SpecialPermission extends ConqueryPermission {

	/**
	 * Generate unique dummy target for a SpecialPermission, since this is the only member that effects a different hash code
	 * for different types of SpecialPermissions.
	 */
	@JsonIgnore
	private final String distinguisher;

	public SpecialPermission() {
		distinguisher = String.format("DUMMY_TARGET_%s", this.getClass().getAnnotation(CPSType.class).id());
	}
	
	@Override
	public Optional<ConqueryPermission> findSimilar(Collection<ConqueryPermission> permissions) {
		Iterator<ConqueryPermission> it = permissions.iterator();
		while (it.hasNext()) {
			ConqueryPermission perm = it.next();
			if(!getClass().isAssignableFrom(perm.getClass())) {
				continue;
			}
			
			return Optional.of(perm);
		}
		return Optional.empty();
	}

}
