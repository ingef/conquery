package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
class StepContext {
	ConversionContext context;
	SqlFunctionProvider sqlFunctions;
	CQConcept node;
	CQTable table;
	String conceptLabel;
	QueryStep previous;
	ConceptSelects previousSelects;
}
