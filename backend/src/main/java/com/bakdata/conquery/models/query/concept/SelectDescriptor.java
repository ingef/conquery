package com.bakdata.conquery.models.query.concept;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Bundles a {@link Select} of a Query with the {@link CQConcept}, it occured in.
 * This is needed for generating more descriptive and individual column names in a CSV.
 */
@Data @AllArgsConstructor
public class SelectDescriptor {
	private Select select;
	private CQConcept cqConcept;

}
