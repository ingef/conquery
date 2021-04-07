package com.bakdata.conquery.models.datasets;

import java.util.Set;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class Dataset extends Labeled<DatasetId> implements Injectable, Authorized {
	public Dataset(String name) {
		setName(name);
	}

	// TODO: 09.01.2020 fk: Maintain concepts in dataset as well, or get rid of tables, but don't do both.

	public static boolean isAllIdsTable(TableId tableId){
		return tableId.getTable().equalsIgnoreCase(ConqueryConstants.ALL_IDS_TABLE);
	}

	@JsonIgnore
	public TableId getAllIdsTableId() {
		return new TableId(getId(), ConqueryConstants.ALL_IDS_TABLE);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues mutableInjectableValues) {
		return mutableInjectableValues.add(Dataset.class, this);
	}

	@Override
	public DatasetId createId() {
		return new DatasetId(getName());
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return DatasetPermission.onInstance(abilities,getId());
	}
}
