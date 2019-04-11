package com.bakdata.conquery.models.concepts;

import java.util.List;

import com.bakdata.conquery.models.concepts.select.Select;

public interface SelectHolder<SELECT extends Select> {
	List<SELECT> getSelects();
	void setSelects(List<SELECT> selects);
	Concept<?> findConcept();
}
