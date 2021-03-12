package com.bakdata.conquery.integration.common;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequiredColumn {
	@NotEmpty
	private String name;
	@NotNull
	private MajorTypeId type;
	private String sharedDictionary;
	@NotEmpty
	private String secondaryId;

	public Column toColumn(Table table, CentralRegistry storage) {
		Column col = new Column();
		col.setName(name);
		col.setType(type);
		col.setSharedDictionary(sharedDictionary);
		col.setTable(table);

		if (secondaryId != null) {
			final SecondaryIdDescription description = storage.resolve(new SecondaryIdDescriptionId(table.getDataset().getId(), secondaryId));

			col.setSecondaryId(description);
		}

		return col;
	}
}
