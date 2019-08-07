package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

import lombok.EqualsAndHashCode;

@CPSType(id="DATASET_PERMISSION", base=ConqueryPermission.class)
@EqualsAndHashCode(callSuper=true)
public class DatasetPermission extends IdentifiableInstancePermission<DatasetId> {

	public DatasetPermission(Set<Ability> abilities, DatasetId instanceId) {
		super(abilities, instanceId);
	}
}
