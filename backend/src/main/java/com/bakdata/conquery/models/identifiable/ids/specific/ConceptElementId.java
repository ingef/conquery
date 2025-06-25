package com.bakdata.conquery.models.identifiable.ids.specific;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public sealed abstract class ConceptElementId<ELEMENT extends ConceptElement<?>> extends NamespacedId<ELEMENT>
		permits ConceptId, ConceptTreeChildId {

	public abstract ConceptId findConcept();

	@JsonIgnore
	public abstract String getName();

	public static enum Parser implements IdUtil.Parser<ConceptElementId<?>> {
		INSTANCE;

		@Override
		public ConceptElementId<?> parseInternally(IdIterator parts) {
			if (parts.remaining() == 2) {
				return ConceptId.Parser.INSTANCE.parse(parts);
			}
			String childName = parts.next();
			ConceptElementId<?> parent = Parser.INSTANCE.parse(parts);
			return new ConceptTreeChildId(parent, childName);
		}
	}
}
