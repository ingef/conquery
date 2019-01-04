package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ConceptId extends ConceptElementId<Concept<?>> implements NamespacedId {

	private final ConceptsId concepts;
	private final String concept;

	@Override
	public DatasetId getDataset() {
		return concepts.getDataset();
	}
	
	@Override
	public ConceptId findConcept() {
		return this;
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		concepts.collectComponents(components);
		components.add(concept);
	}
	
	public static enum Parser implements IId.Parser<ConceptId> {
		INSTANCE;
		
		@Override
		public ConceptId parse(Iterator<String> parts) {
			ConceptsId parent = ConceptsId.Parser.INSTANCE.parse(parts);
			return new ConceptId(parent, parts.next());
		}
	}
}
