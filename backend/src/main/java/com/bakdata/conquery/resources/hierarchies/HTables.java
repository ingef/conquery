package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.models.datasets.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
public abstract class HTables extends HDatasets {

	@PathParam(TABLE)
	protected Table table;
}