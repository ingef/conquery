package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

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
	public ConceptId findConcept() {
		return parent.findConcept();
	}

	@Override
	public void collectComponents(List<Object> components) {
		parent.collectComponents(components);
		components.add(name);
	}
	
	public static enum Parser implements IId.Parser<ConceptTreeChildId> {
		INSTANCE;
		
		@Override
		public ConceptTreeChildId parseInternally(IdIterator parts) {
			String childName = parts.next();
			ConceptElementId<?> parent = ConceptElementId.Parser.INSTANCE.parse(parts);
			return new ConceptTreeChildId(parent, childName);
		}
	}
}
