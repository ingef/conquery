package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

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

	public enum Parser implements AId.Parser<ConceptSelectId> {
		INSTANCE;

		@Override
		public ConceptSelectId parseInternally(IdIterator parts) {
			String name = parts.next();
			ConceptId parent = ConceptId.Parser.INSTANCE.parse(parts);
			return new ConceptSelectId(parent, name);
		}
	}

	@Override
	public ConceptId findConcept() {
		return concept;
	}
}