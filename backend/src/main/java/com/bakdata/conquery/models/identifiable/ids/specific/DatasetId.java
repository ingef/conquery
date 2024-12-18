package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
@EqualsAndHashCode(callSuper = false, doNotUseGetters = true, onlyExplicitlyIncluded = true)
public class DatasetId extends NamespacedId<Dataset> implements Authorized {

	@ToString.Include
	@EqualsAndHashCode.Include
	private final String name;

	/**
	 * Injected by deserializer
	 */
	@JsonIgnore
	@Setter(onParam_ = {@NonNull})
	@Getter
	private NamespacedStorage storage;

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return this;
	}

	@Override
	public Dataset get() {
		return getStorage().getDataset();
	}


	@Override
	public void collectComponents(List<Object> components) {
		components.add(name);
	}

	@Override
	public void collectIds(Collection<? super Id> into) {
		into.add(this);
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
