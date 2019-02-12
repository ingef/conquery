package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Iterator;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ConceptElementId<T extends ConceptElement<?>> extends AId<T> implements NamespacedId {

	public abstract ConceptId findConcept();
	
	@JsonIgnore
	public abstract String getName();
	
	public static enum Parser implements IId.Parser<ConceptElementId<?>> {
		INSTANCE;
		
		@Override
		public ConceptElementId<?> parse(Iterator<String> parts) {
			ConceptElementId<?> result = ConceptId.Parser.INSTANCE.parse(parts);
			while(parts.hasNext()) {
				result = new ConceptTreeChildId(result, parts.next());
			}
			return result;
		}
	}
}
