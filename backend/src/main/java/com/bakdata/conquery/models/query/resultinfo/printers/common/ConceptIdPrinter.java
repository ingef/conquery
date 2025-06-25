package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import org.jetbrains.annotations.NotNull;

public record ConceptIdPrinter(TreeConcept concept, PrintSettings cfg) implements Printer<Integer> {

	@Override
	public String apply(@NotNull Integer localId) {

		final ConceptElement<?> node = concept.getElementByLocalId(localId);

		if (!cfg.isPrettyPrint()) {
			return node.getId().toString();
		}

		if (node.getDescription() == null) {
			return node.getLabel();
		}

		return node.getLabel() + " - " + node.getDescription();
	}
}
