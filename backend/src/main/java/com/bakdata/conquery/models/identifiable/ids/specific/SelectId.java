package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public abstract class SelectId extends AId<Select> implements NamespacedId {

	private final String select;
	
	public abstract ConceptId findConcept();

	@Override
	public void collectComponents(List<Object> components) {
		components.add(select);
	}

	public static enum Parser implements IId.Parser<SelectId> {
		INSTANCE;

		@Override
		public SelectId parseInternally(IdIterator parts) {
			if (parts.remaining() == 3) {
				return ConceptSelectId.Parser.INSTANCE.parse(parts);
			}
			return ConnectorSelectId.Parser.INSTANCE.parse(parts);
		}
	}
}