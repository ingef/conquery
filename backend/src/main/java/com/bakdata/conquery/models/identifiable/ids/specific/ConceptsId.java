package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.models.concepts.Concepts;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ConceptsId extends AId<Concepts> implements NamespacedId {

	private final DatasetId dataset;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add("concepts");
	}
	
	public static enum Parser implements IId.Parser<ConceptsId> {
		INSTANCE;
		
		@Override
		public ConceptsId parse(Iterator<String> parts) {
			DatasetId parent = DatasetId.Parser.INSTANCE.parse(parts);
			String nextPart = parts.next();
			if (!"concepts".equals(nextPart)) {
				throw new IllegalStateException("ConceptsId expects part 'concepts' but found '" + nextPart + "'");
			}
			return new ConceptsId(parent);
		}
	}
}
