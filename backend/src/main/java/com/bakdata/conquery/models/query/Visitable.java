package com.bakdata.conquery.models.query;

import java.util.function.Consumer;

public interface Visitable {

	void visit(Consumer<Visitable> visitor);
	
}
