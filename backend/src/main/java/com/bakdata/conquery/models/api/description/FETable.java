package com.bakdata.conquery.models.api.description;

import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * This class represents a concept filter parameter as it is presented to the front end.
 */
@Data @Builder
public class FETable {
	private ConnectorId id;
	private String label;
	private List<FEFilter> filters;
	private List<FESelect> selects;
}
