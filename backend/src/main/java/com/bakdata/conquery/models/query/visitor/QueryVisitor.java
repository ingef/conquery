package com.bakdata.conquery.models.query.visitor;

import com.bakdata.conquery.models.query.concept.specific.CQConcept;

public interface QueryVisitor {

	public default void visitConcept(CQConcept concept) {}
}
