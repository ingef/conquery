package com.bakdata.conquery.sql.conversion.cqelement.concept.model;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Filters {

	@Builder.Default
	List<FilterCondition> event = Collections.emptyList();
	@Builder.Default
	List<FilterCondition> group = Collections.emptyList();

	public Filters negated() {
		return new Filters(
				event.stream().map(FilterCondition::negate).toList(),
				group.stream().map(FilterCondition::negate).toList()
		);
	}

}
