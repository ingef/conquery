package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @EqualsAndHashCode(callSuper=true)
public class ConceptSelectId extends SelectId {

	private final ConceptId concept;
	
	public ConceptSelectId(ConceptId concept, String select) {
		super(select);
		this.concept = concept;
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
		concept.collectIds(collect);
	}

	@Override
	public DatasetId getDataset() {
		return concept.getDataset();
	}

	@Override
	public Select get() {
		return getStorage().getConcept(concept).getSelectByName(getSelect());
	}

	@Override
	public ConceptId findConcept() {
		return concept;
	}

	@Override
	public void collectComponents(List<Object> components) {
		concept.collectComponents(components);
		super.collectComponents(components);
	}

	public enum Parser implements IdUtil.Parser<ConceptSelectId> {
		INSTANCE;

		@Override
		public ConceptSelectId parseInternally(IdIterator parts) {
			String name = parts.next();
			ConceptId parent = ConceptId.Parser.INSTANCE.parse(parts);
			return new ConceptSelectId(parent, name);
		}
	}
}