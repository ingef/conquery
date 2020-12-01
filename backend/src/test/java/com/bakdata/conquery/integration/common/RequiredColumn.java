package com.bakdata.conquery.integration.common;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryId;
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
	private SecondaryId secondaryId;

	public Column toColumn(Table t) {
		Column col = new Column();
		col.setName(name);
		col.setType(type);
		col.setSharedDictionary(sharedDictionary);
		col.setTable(t);
		col.setSecondaryId(secondaryId);
		return col;
	}
}
