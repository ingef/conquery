package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
public abstract class HTables extends HDatasets {
	
	@PathParam(TABLE)
	protected TableId tableId;
	protected Table table;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.table = namespace
			.getStorage()
			.getDataset()
			.getTables()
			.getOptional(tableId)
			.orElseThrow(() -> new WebApplicationException("Could not find table "+tableId, Status.NOT_FOUND));
	}
}