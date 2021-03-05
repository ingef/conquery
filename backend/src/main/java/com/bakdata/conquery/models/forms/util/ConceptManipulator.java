package com.bakdata.conquery.models.forms.util;

import java.util.List;

import com.bakdata.conquery.models.forms.util.DefaultSelectConceptManipulator.FillMethod;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.DatasetRegistry;

public interface ConceptManipulator {
	// Often used manipulators, that can be statically instantiated
	public final static ConceptManipulator DEFAULT_SELECTS_WHEN_EMPTY = new DefaultSelectConceptManipulator(FillMethod.ADD_TO_COMPLETE_EMPTY);
	
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
