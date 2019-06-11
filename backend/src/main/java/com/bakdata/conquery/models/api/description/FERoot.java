package com.bakdata.conquery.models.api.description;

import java.util.LinkedHashMap;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.ids.IId;

import lombok.Getter;

/**
 * This class represents the root node of the concepts as it is presented to the front end.
 */
@Getter
public class FERoot {
	private Map<IId<?>, FENode> concepts  = new LinkedHashMap<>();
}
