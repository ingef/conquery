package com.bakdata.conquery.sql.conversion.cqelement.concept.model;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.InvertedCondition;
import org.jooq.Condition;

public interface FilterCondition {

	Condition filterCondition();

	FilterType type();

	default FilterCondition invert() {
		return new InvertedCondition(this);
	}

}
