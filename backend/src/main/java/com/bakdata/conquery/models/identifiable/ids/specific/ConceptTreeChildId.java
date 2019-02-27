package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.google.common.collect.PeekingIterator;

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
		public ConceptTreeChildId parse(PeekingIterator<String> parts) {
			ConceptId parent = ConceptId.Parser.INSTANCE.parse(parts);
			ConceptTreeChildId result = new ConceptTreeChildId(parent, parts.next());
			while(parts.hasNext()) {
				result = new ConceptTreeChildId(result, parts.next());
			}
			return result;
		}
	}
}
