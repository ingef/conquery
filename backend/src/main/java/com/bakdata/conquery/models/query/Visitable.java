package com.bakdata.conquery.models.query;

import java.util.function.Consumer;

import com.bakdata.conquery.models.query.concept.CQElement;

public interface Visitable {

	void visit(Consumer<CQElement> visitor);
	
}
