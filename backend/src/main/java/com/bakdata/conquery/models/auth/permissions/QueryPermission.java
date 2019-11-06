package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableSet;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@CPSType(id="QUERY_PERMISSION", base=ConqueryPermission.class)
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class QueryPermission extends IdentifiableInstancePermission<ManagedExecutionId>{

	public final static Set<Ability> ALLOWED_ABILITIES = ImmutableSet.copyOf(AbilitySets.QUERY_CREATOR);
	
	@JsonCreator
	public QueryPermission(Set<Ability> abilities, ManagedExecutionId instanceId) {
		super(abilities, instanceId);
	}

	@Override
	public Set<Ability> allowedAbilities() {
		return ALLOWED_ABILITIES;
	}
}
