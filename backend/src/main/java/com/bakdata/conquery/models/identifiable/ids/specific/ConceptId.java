package com.bakdata.conquery.models.identifiable.ids.specific;

import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ConceptId extends ConceptElementId<Concept<?>> implements NamespacedId {

	private final DatasetId dataset;
	private final String name;

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
		components.add(name);
	}
	
	public static enum Parser implements IId.Parser<ConceptId> {
		INSTANCE;
		
		@Override
		public ConceptId parseInternally(IdIterator parts) {
			String name = parts.next();
			DatasetId parent = DatasetId.Parser.INSTANCE.parse(parts);
			return new ConceptId(parent, name);
		}
	}
}
