package com.bakdata.conquery.models.query.visitor;

import com.bakdata.conquery.models.query.concept.CQElement;

/**
 * Visits the elements of which a query consist.
 */
public interface QueryVisitor {
	
	void visit(CQElement element);
}
