package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;

public interface Visitable {

	void visit(QueryVisitor visitor);
	
	
	/**
	 * Checks if the query requires to resolve external ids.
	 * @return True if a {@link CQExternal} is found.
	 */
	static boolean usesExternalIds(Visitable query) {
		
		final List<CQExternal> elements = new ArrayList<>();
		
		query.visit(new QueryVisitor() {
			
			@Override
			public void visit(CQElement element) {
				if (element instanceof CQExternal) {	
					elements.add((CQExternal)element);
				}
			}
		});
		
		return elements.size() > 0;
	}
}
