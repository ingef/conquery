package com.bakdata.conquery.sql.conversion.cqelement.concept.model;

import lombok.Value;

@Value
public class ConceptFilter {
	SqlSelects selects;
	Filters filters;
}
