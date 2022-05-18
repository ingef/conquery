package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false, doNotUseGetters=true)
public class DatasetId extends AId<Dataset> implements NamespacedId {

	private final String name;
	
	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return this;
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		components.add(name);
	}

	public static enum Parser implements IdUtil.Parser<DatasetId> {
		INSTANCE;

		@Override
		public DatasetId parseInternally(IdIterator parts) {
			return new DatasetId(parts.next());
		}
	}
}
