package com.bakdata.conquery.models.datasets;

import java.util.Set;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class Dataset extends Labeled<DatasetId> implements Injectable, Authorized, NamespacedIdentifiable<DatasetId> {
	public Dataset(String name) {
		setName(name);
	}

	/**
	 * Used to programmatically generate proper {@link com.bakdata.conquery.models.identifiable.ids.NamespacedId}s.
	 */
	public static final Dataset PLACEHOLDER = new Dataset("PLACEHOLDER");

	public static boolean isAllIdsTable(Table table){
		return table.getName().equalsIgnoreCase(ConqueryConstants.ALL_IDS_TABLE);
	}

	/**
	 * Sorting weight for Frontend.
	 */
	@InternalOnly
	private int weight;

	@JsonIgnore
	public Table getAllIdsTable() {
		//TODO store this somehow? / Add this at dataset creation
		final Table table = new Table();
		table.setDataset(this);
		table.setName(ConqueryConstants.ALL_IDS_TABLE);
		return table;
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

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return this;
	}
}
