package com.bakdata.conquery.apiv1.frontend;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.Getter;

/**
 * This class represents the root node of the concepts as it is presented to the front end.
 */
@Getter
public class FERoot {
	private Set<FESecondaryId> secondaryIds = new HashSet<>();
	private Map<IId<?>, FENode> concepts  = new LinkedHashMap<>();
}
