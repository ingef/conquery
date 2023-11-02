package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import lombok.Value;
import org.jooq.Field;

@Value
public class IntervalPackingContext implements Context {

	String nodeLabel;
	Field<Object> primaryColumn;
	ColumnDateRange validityDate;
	IntervalPackingTables intervalPackingTables;

	public IntervalPackingContext(
			String nodeLabel,
			Field<Object> primaryColumn,
			ColumnDateRange validityDate,
			ConceptTables conceptTables
	) {
		this.nodeLabel = nodeLabel;
		this.primaryColumn = primaryColumn;
		this.validityDate = validityDate;
		this.intervalPackingTables = IntervalPackingTables.forConcept(nodeLabel, conceptTables);
	}

}
