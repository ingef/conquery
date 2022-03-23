package com.bakdata.conquery.models.datasets.concepts.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public interface GroupFilter {

	JavaType getFilterValueType(TypeFactory tf);
}
