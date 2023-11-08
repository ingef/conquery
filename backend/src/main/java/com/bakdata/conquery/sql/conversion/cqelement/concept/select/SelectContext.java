package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Value;

@Value
public class SelectContext implements Context {
	ConversionContext parentContext;
	CQConcept concept;
	String label;
	Optional<ColumnDateRange> validityDate;
	SqlTables<ConceptCteStep> conceptTables;
}
