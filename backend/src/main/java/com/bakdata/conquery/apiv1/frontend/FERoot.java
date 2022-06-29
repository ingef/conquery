package com.bakdata.conquery.apiv1.frontend;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.Id;
import lombok.Getter;

/**
 * This class represents the root node of the concepts as it is presented to the front end.
 */
@Getter
public class FERoot {
	private final Set<FESecondaryId> secondaryIds = new HashSet<>();
	private final Map<Id<?>, @Valid FENode> concepts = new LinkedHashMap<>();
}
