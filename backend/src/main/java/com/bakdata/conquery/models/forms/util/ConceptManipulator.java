package com.bakdata.conquery.models.forms.util;

import java.util.List;

import c10n.annotations.De;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.DatasetRegistry;

@Deprecated
public interface ConceptManipulator {
	// Often used manipulators, that can be statically instantiated

	void consume(CQConcept concept, DatasetRegistry namespaces);
	
	default void consume(List<? extends CQElement> userFeatures, DatasetRegistry namespaces) {
		for(CQElement feature : userFeatures) {
			feature.visit(element -> {
				if (element instanceof CQConcept) {
					consume((CQConcept) element, namespaces);
				}
			});
		}
	}
}
