package com.bakdata.conquery.apiv1.frontend;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a concept filter as it is presented to the front end.
 */
@Data @Builder
public class FESelect {
	private SelectId id;
	private String label;
	private String description;
	private ResultType resultType;
}
