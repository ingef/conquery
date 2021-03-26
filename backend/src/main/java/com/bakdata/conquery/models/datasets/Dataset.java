package com.bakdata.conquery.models.datasets;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class Dataset extends Labeled<DatasetId> implements Injectable {

	/**
	 * Used to programmatically generate proper Namespaced Ids.
	 */
	public static final Dataset PLACEHOLDER = new Dataset("PLACEHOLDER");

	public Dataset(String name){
		setName(name);
	}


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
