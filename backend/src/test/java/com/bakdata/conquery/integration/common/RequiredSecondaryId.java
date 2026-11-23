package com.bakdata.conquery.integration.common;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Objects;

@Getter
@RequiredArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.PROPERTIES))
public class RequiredSecondaryId {
	@NotEmpty
	public final String name;

	public final String label;
	public final String description;

	public final String mapping;

	public SecondaryIdDescription toSecondaryId(DatasetId dataset) {
		final SecondaryIdDescription desc = new SecondaryIdDescription();

		desc.setName(getName());
		desc.setDescription(getDescription());
		desc.setLabel(getLabel());
		if (mapping != null) {
			desc.setMapping(new InternToExternMapperId(dataset, mapping));
		}

		return desc;
	}


	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static RequiredSecondaryId fromFile(String fileResource) throws IOException {
		return Jackson.MAPPER.readValue(
				Objects.requireNonNull(
						IntegrationTest.class.getResourceAsStream(fileResource),
						fileResource + " not found"
				),
				RequiredSecondaryId.class
		);
	}
}
