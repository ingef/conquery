package com.bakdata.conquery.models.forms.util;

import java.util.List;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;

@Deprecated
public interface ConceptManipulator {
	// Often used manipulators, that can be statically instantiated

	void consume(CQConcept concept, DatasetRegistry namespaces);

	default void consume(List<? extends CQElement> userFeatures, DatasetRegistry namespaces) {
		// Visit all Features that are CQConcept
		userFeatures.stream()
					.flatMap(Visitable::stream)
					.filter(CQConcept.class::isInstance)
					.map(CQConcept.class::cast)
					.forEach(concept -> consume(userFeatures, namespaces));
	}
}
