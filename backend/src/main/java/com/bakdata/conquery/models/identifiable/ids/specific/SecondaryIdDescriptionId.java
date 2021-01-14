package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class SecondaryIdDescriptionId extends AId<SecondaryIdDescription> implements NamespacedId {

	private final DatasetId dataset;
	private final String name;
	
	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(name);
	}
	
	public static enum Parser implements IId.Parser<SecondaryIdDescriptionId> {
		INSTANCE;
		
		@Override
		public SecondaryIdDescriptionId parseInternally(IdIterator parts) {
			String name = parts.next();
			DatasetId dataset = DatasetId.Parser.INSTANCE.parse(parts);
			return new SecondaryIdDescriptionId(dataset, name);
		}
	}
}
