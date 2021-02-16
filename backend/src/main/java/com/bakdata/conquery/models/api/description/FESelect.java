package com.bakdata.conquery.models.api.description;

import com.bakdata.conquery.models.externalservice.SimpleResultType;
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
	private SimpleResultType resultType;
}
