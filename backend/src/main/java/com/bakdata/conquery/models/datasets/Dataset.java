package com.bakdata.conquery.models.datasets;

import javax.validation.Valid;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.concepts.Concepts;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Dataset extends Labeled<DatasetId> implements Injectable {

	@JsonManagedReference @Valid
	private IdMap<TableId, Table> tables = new IdMap<>();
	
	@JsonManagedReference @Valid
	private Concepts concepts = new Concepts();
	
	public Dataset() {
		concepts.setDataset(this);
	}
	
	@Override
	public MutableInjectableValues inject(MutableInjectableValues mutableInjectableValues) {
		return mutableInjectableValues.add(Dataset.class, this);
	}

	@Override
	public DatasetId createId() {
		return new DatasetId(getName());
	}
}