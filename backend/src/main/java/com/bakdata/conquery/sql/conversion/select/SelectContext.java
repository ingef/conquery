package com.bakdata.conquery.sql.conversion.select;

import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTableNames;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import lombok.Value;

@Value
public class SelectContext implements Context {
	ConversionContext parentContext;
	CQConcept concept;
	String label;
	Optional<ColumnDateRange> validityDate;
	ConceptTableNames conceptTableNames;
}
