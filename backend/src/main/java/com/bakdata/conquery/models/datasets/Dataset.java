package com.bakdata.conquery.models.datasets;

import java.util.Set;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.LabeledNamespaceIdentifiable;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Dataset extends LabeledNamespaceIdentifiable<DatasetId> implements Injectable, Authorized {

	/**
	 * Used to programmatically generate proper {@link com.bakdata.conquery.models.identifiable.ids.NamespacedId}s.
	 */
	public static final Dataset PLACEHOLDER = new Dataset("PLACEHOLDER");
	/**
	 * Sorting weight for Frontend.
	 */
	private int weight;

	/**
	 * Resolver for {@link com.bakdata.conquery.models.identifiable.ids.NamespacedId}s.
	 * It is usually injected when this object is loaded from a store, or set manually, when it is created.
	 **/
	@JacksonInject(useInput = OptBoolean.FALSE)
	@Getter
	@Setter
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private NamespacedStorageProvider storageProvider;


	public Dataset(String name) {
		setName(name);
	}

	public static boolean isAllIdsTable(Table table) {
		return table.getName().equalsIgnoreCase(ConqueryConstants.ALL_IDS_TABLE);
	}

	@JsonIgnore
	public Table getAllIdsTable() {
		// TODO migrate to NamespaceStorage
		final Table table = new Table();
		table.setNamespacedStorageProvider(storageProvider.getStorage(getDataset()));
		table.setName(ConqueryConstants.ALL_IDS_TABLE);
		table.init();

		// We could use the resolvers of this dataset, but actually this table's id should never be resolved
		return table;
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues mutableInjectableValues) {
		return mutableInjectableValues
				.add(Dataset.class, this)
				.add(DatasetId.class, getId());
	}

	@Override
	public DatasetId createId() {
		return new DatasetId(getName());
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return getId().createPermission(abilities);
	}

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return getId();
	}

	@Override
	public NamespacedStorageProvider getDomain() {
		return getStorageProvider();
	}
}
