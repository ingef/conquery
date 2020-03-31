package com.bakdata.conquery.commands;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.resources.ResourceConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING })
@Produces({ExtraMimeTypes.JSON_STRING})
@Path("worker/{" + ResourceConstants.DATASET + "}")
@RequiredArgsConstructor
@Getter
public class WorkerAPI {

	private final Workers workers;

	@GET
	@Path("tables")
	public List<Table> getTables(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		return new ArrayList<>(workers.getWorkerForDataset(datasetId).getStorage().getDataset().getTables().values());
	}

	@GET
	@Path("concepts")
	public List<? extends Concept<?>> getConcepts(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		return new ArrayList<>(workers.getWorkerForDataset(datasetId).getStorage().getAllConcepts());
	}
}
