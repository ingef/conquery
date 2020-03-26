package com.bakdata.conquery.resources.admin.slave;


import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Workers;
import lombok.Getter;
import lombok.Setter;


@Produces(MediaType.APPLICATION_JSON)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Getter
@Setter
@Path("workers/{" + DATASET + "}/")
public class WorkerAPIResource {

	@PathParam(DATASET)
	private DatasetId datasetId;

	@Inject
	private Workers workers;

	@GET
	@Path("tables")
	public Collection<Table> getDataset() {
		return workers.getDatasetWorker(datasetId).getStorage().getDataset().getTables().values();
	}

	@GET
	@Path("concepts")
	public Collection<? extends Concept> getConcepts() {
		return workers.getDatasetWorker(datasetId).getStorage().getAllConcepts();
	}
}