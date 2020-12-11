package com.bakdata.conquery.integration.common;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import lombok.Data;

@Data
public class RequiredSecondaryIds {
	@NotEmpty
	public final String name;

	public final String label;
	public final String description;

	public SecondaryIdDescription toSecondaryId() {
		final SecondaryIdDescription desc = new SecondaryIdDescription();

		desc.setName(getName());
		desc.setDescription(getDescription());
		desc.setLabel(getLabel());

		return desc;
	}
}
