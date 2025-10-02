package com.bakdata.conquery.integration.common;

import javax.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.util.Strings;

@Data
@NoArgsConstructor(onConstructor_ = {@JsonCreator})
public class RequiredColumn {

	public RequiredColumn(String name, MajorTypeId type) {
		this.name = name;
		this.type = type;
	}

	@NotEmpty
	private String name;
	@NotNull
	private MajorTypeId type;

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

	public Column toColumn(Table table, NamespacedStorageProvider idResolver) {
		Column col = new Column();
		col.setName(name);
		col.setType(type);
		col.setTable(table);
		col.setDescription(description);

		if (!Strings.isNullOrEmpty(secondaryId)) {
			SecondaryIdDescriptionId secondaryIdDescriptionId = new SecondaryIdDescriptionId(table.getDataset(), secondaryId);
			secondaryIdDescriptionId.setDomain(idResolver);
			final SecondaryIdDescription description = secondaryIdDescriptionId.resolve();

			col.setSecondaryId(description.getId());
		}

		return col;
	}
}
