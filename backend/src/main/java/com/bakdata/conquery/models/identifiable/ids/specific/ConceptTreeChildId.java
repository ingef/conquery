package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ConceptTreeChildId extends ConceptElementId<ConceptTreeChild> implements NamespacedId {

	private final ConceptElementId<?> parent;
	private final String name;
	
	@Override
	public DatasetId getDataset() {
		return parent.getDataset();
	}

	@Override
	public NamespacedIdentifiable<?> get(NamespacedStorage storage) {
		Concept<?> concept = storage.getConcept(findConcept());
		if (concept == null) {
			return null;
		}
		return concept.findById(this);
	}

	@Override
	public ConceptId findConcept() {
		return parent.findConcept();
	}

	@Override
	public void collectComponents(List<Object> components) {
		parent.collectComponents(components);
		components.add(name);
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
		parent.collectIds(collect);
	}

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return parent.getNamespacedStorageProvider();
	}

	public static enum Parser implements IdUtil.Parser<ConceptTreeChildId> {
		INSTANCE;

		@Override
		public ConceptTreeChildId parseInternally(IdIterator parts) {
			String childName = parts.next();
			ConceptElementId<?> parent = ConceptElementId.Parser.INSTANCE.parse(parts);
			return new ConceptTreeChildId(parent, childName);
		}
	}
}
