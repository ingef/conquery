package com.bakdata.conquery.integration.common;

import java.io.IOException;
import java.util.Objects;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Mappers;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.Data;

@Data
public class RequiredSecondaryId {
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


	@JsonCreator
	public static RequiredSecondaryId fromFile(String fileResource) throws JsonParseException, JsonMappingException, IOException {
		return Mappers.getMapper().readValue(
				Objects.requireNonNull(
						IntegrationTest.class.getResourceAsStream(fileResource),
						fileResource+" not found"
				),
				RequiredSecondaryId.class
		);
	}
}
