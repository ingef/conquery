package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class DatasetId extends NamespacedId<Dataset> implements Authorized {

	@EqualsAndHashCode.Include
	private final String name;

	@Setter
	@JacksonInject(useInput = OptBoolean.FALSE)
	@JsonIgnore
	private NamespacedStorageProvider namespacedStorageProvider;

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return this;
	}

	@Override
	public void setDomain(NamespacedStorageProvider provider) {
		setNamespacedStorageProvider(provider);
	}

	@Override
	public Dataset get(NamespacedStorageProvider storage) {
		return storage.getStorage(this).getDataset();
	}

	@Override
	public void collectComponents(List<Object> components) {
		components.add(name);
	}

	@Override
	public void collectIds(Collection<Id<?, ?>> collect) {
		collect.add(this);
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return DatasetPermission.onInstance(abilities, this);
	}

	public static enum Parser implements IdUtil.Parser<DatasetId> {
		INSTANCE;

		@Override
		public DatasetId parseInternally(IdIterator parts) {
			return new DatasetId(parts.next());
		}
	}
}
