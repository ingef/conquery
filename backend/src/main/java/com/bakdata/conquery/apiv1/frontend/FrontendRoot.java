package com.bakdata.conquery.apiv1.frontend;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import jakarta.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.Id;
import lombok.Getter;

/**
 * This class represents the root node of the concepts as it is presented to the front end.
 */
@Getter
public class FrontendRoot {
	private final Set<FrontendSecondaryId> secondaryIds = new HashSet<>();
	/**
	 * Can be:
	 * 	- {@link com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId}
	 * 	- {@link com.bakdata.conquery.models.identifiable.ids.specific.ConceptId}
	 */
	private final Map<Id<?, ?>, @Valid FrontendNode> concepts = new LinkedHashMap<>();
}
