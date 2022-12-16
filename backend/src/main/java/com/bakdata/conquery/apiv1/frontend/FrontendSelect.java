package com.bakdata.conquery.apiv1.frontend;

import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a concept filter as it is presented to the front end.
 */
@Data
@Builder
public class FrontendSelect {
	private SelectId id;
	private String label;
	private String description;
	private ResultType resultType;
	@JsonProperty("default")
	private Boolean isDefault;
}
