package com.bakdata.conquery.integration.common;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import lombok.Getter;
import lombok.Setter;
import org.assertj.core.util.Strings;

@Getter
@Setter
public class RequiredColumn {
	@NotEmpty
	private String name;
	@NotNull
	private MajorTypeId type;
	private String sharedDictionary;

	@Nullable
	private String description;

	private String secondaryId;

	@Nullable
	private OutputDescription outputDescription;

	public OutputDescription createOutput() {
		if (outputDescription != null) {
			return outputDescription;
		}
		CopyOutput out = new CopyOutput();
		out.setInputColumn(getName());
		out.setInputType(getType());
		out.setName(getName());
		return out;
	}

	public Column toColumn(Table table, CentralRegistry storage) {
		Column col = new Column();
		col.setName(name);
		col.setType(type);
		col.setSharedDictionary(sharedDictionary);
		col.setTable(table);
		col.setDescription(description);

		if (!Strings.isNullOrEmpty(secondaryId)) {
			final SecondaryIdDescription description = storage.resolve(new SecondaryIdDescriptionId(table.getDataset().getId(), secondaryId));

			col.setSecondaryId(description);
		}

		return col;
	}
}
