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

	private final DatasetId dataset;
	private final String concept;

	@Override
	public DatasetId getDataset() {
		return dataset;
	}
	
	@Override
	public ConceptId findConcept() {
		return this;
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(concept);
	}
	
	public static enum Parser implements IId.Parser<ConceptId> {
		INSTANCE;
		
		@Override
		public ConceptId parse(Iterator<String> parts) {
			DatasetId parent = DatasetId.Parser.INSTANCE.parse(parts);
			return new ConceptId(parent, parts.next());
		}
	}
}
