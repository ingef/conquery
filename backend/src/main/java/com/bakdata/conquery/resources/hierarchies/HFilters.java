package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}/filters/{" + FILTER + "}")
public abstract class HFilters extends HConnectors {

	@PathParam(FILTER)
	protected Filter<?> filter;
}