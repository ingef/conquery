package com.bakdata.conquery.models.query.resultinfo.printers;

import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.PrintSettings;

public record ConceptIdPrinter(Concept concept, PrintSettings cfg) implements Printer {

	@Override
	public String print(Object rawValue) {
		if (rawValue == null) {
			return null;
		}

		final int localId = (int) rawValue;

		final ConceptTreeNode<?> node = ((TreeConcept) concept).getElementByLocalId(localId);

		if (!cfg.isPrettyPrint()) {
			return node.getId().toString();
		}

		if (node.getDescription() == null) {
			return node.getLabel();
		}

		return node.getLabel() + " - " + node.getDescription();
	}
}
