package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConceptPermission;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ConceptId extends ConceptElementId<Concept<?>> implements Authorized {

	private final DatasetId dataset;
	private final String name;

	@Override
	public DatasetId getDataset() {
		return dataset;
	}

	@Override
	public Concept<?> get(NamespacedStorage storage) {
		return storage.getConcept(this);
	}

	@Override
	public ConceptId findConcept() {
		return this;
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(name);
	}


	@Override
	public void collectIds(Collection<Id<?,?>> collect) {
		collect.add(this);
		dataset.collectIds(collect);
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return ConceptPermission.onInstance(abilities, this);
	}

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return dataset.getNamespacedStorageProvider();
	}

	public enum Parser implements IdUtil.Parser<ConceptId> {
		INSTANCE;

		@Override
		public ConceptId parseInternally(IdIterator parts) {
			String name = parts.next();
			DatasetId parent = DatasetId.Parser.INSTANCE.parse(parts);
			return new ConceptId(parent, name);
		}
	}
}
