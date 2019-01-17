package com.bakdata.conquery.util;

import java.util.List;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;

public class ConceptsUtils {

	public static IdMap<ConnectorId, Connector> getAllConnectors(List<Concept<?>> concepts) {
		IdMap<ConnectorId, Connector> result = new IdMap<>();

		for (Concept c : concepts) {
			result.addAll(c.getConnectors());
		}

		return result;
	}
}
