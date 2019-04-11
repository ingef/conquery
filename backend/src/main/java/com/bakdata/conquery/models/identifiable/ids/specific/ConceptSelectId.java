package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.google.common.collect.PeekingIterator;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @EqualsAndHashCode(callSuper=true)
public class ConceptSelectId extends SelectId implements NamespacedId {

	private final ConceptId concept;
	
	public ConceptSelectId(ConceptId concept, String select) {
		super(select);
		this.concept = concept;
	}

	@Override
	public void collectComponents(List<Object> components) {
		concept.collectComponents(components);
		super.collectComponents(components);
	}

	@Override
	public DatasetId getDataset() {
		return concept.getDataset();
	}

	public enum Parser implements IId.Parser<ConceptSelectId> {
		INSTANCE;
		
		@Override
		public ConceptSelectId parse(PeekingIterator<String> parts) {
			ConceptId parent = ConceptId.Parser.INSTANCE.parse(parts);
			return new ConceptSelectId(parent, parts.next());
		}
	}
}