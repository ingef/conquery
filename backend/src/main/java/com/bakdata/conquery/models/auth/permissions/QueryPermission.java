package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.EqualsAndHashCode;

@CPSType(id="QUERY_PERMISSION", base=ConqueryPermission.class)
@EqualsAndHashCode(callSuper=true)
public class QueryPermission extends IdentifiableInstancePermission<ManagedExecutionId>{
	
	@JsonCreator
	public QueryPermission(Set<Ability> abilities, ManagedExecutionId instanceId) {
		super(abilities, instanceId);
	}
}
