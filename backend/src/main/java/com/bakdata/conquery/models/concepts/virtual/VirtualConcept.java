package com.bakdata.conquery.models.concepts.virtual;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;

/**
 * This is a single node or concept in a concept tree.
 */
@CPSType(id="VIRTUAL", base=Concept.class)
public class VirtualConcept extends Concept<VirtualConceptConnector> {
	
	private static final long serialVersionUID = 1L;

}
