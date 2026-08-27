package com.bakdata.conquery.integration.json.filter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import lombok.Data;

@Data
public class FilterDescription<FE_TYPE extends FilterValue<?>> {

	@NotNull
	@Valid
	private List<Concept<?>> concepts;

	//	@NotNull @Valid
	//	private VirtualConceptConnector connector;

	@NotNull
	@Valid
	private FE_TYPE value;
}
