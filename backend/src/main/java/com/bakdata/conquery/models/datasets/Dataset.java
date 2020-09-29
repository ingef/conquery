package com.bakdata.conquery.models.datasets;

import javax.validation.Valid;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Dataset extends Labeled<DatasetId> implements Injectable {

	@JsonManagedReference @Valid
	private IdMap<TableId, Table> tables = new IdMap<>();

	// TODO: 09.01.2020 fk: Maintain concepts in dataset as well, or get rid of tables, but don't do both.

	public static boolean isAllIdsTable(TableId tableId){
		return tableId.getTable().equalsIgnoreCase(ConqueryConstants.ALL_IDS_TABLE);
	}

	@JsonIgnore
	public TableId getAllIdsTableId() {
		return new TableId(getId(), ConqueryConstants.ALL_IDS_TABLE);
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
