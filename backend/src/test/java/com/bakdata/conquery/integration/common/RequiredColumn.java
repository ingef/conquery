package com.bakdata.conquery.integration.common;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.google.common.base.Strings;
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
	private String secondaryId;

	public Column toColumn(Table t) {
		Column col = new Column();
		col.setName(name);
		col.setType(type);
		col.setSharedDictionary(sharedDictionary);
		col.setTable(t);

		if(!Strings.isNullOrEmpty(secondaryId)) {
			final SecondaryIdDescription description = new SecondaryIdDescription();
			description.setDataset(t.getDataset());
			description.setName(secondaryId);

			col.setSecondaryId(description);
		}

		return col;
	}
}
