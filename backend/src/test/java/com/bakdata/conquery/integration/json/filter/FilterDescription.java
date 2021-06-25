package com.bakdata.conquery.integration.json.filter;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;

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