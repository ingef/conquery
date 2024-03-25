package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Optional;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptConversionTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import lombok.Value;

@Value
public class SelectContext implements Context {

	SqlIdColumns ids;
	Optional<ColumnDateRange> validityDate;
	ConceptConversionTables tables;
	ConversionContext conversionContext;

}
