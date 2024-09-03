package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;

public record ConceptIdPrinter(Concept concept, PrintSettings cfg) implements Printer<Integer> {

	@Override
	public String apply(Integer localId) {
		if (localId == null) {
			return null;
		}

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
