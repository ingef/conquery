package com.bakdata.conquery.commands;

import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
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
	public Response getTables(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		if(workers.getWorkerForDataset(datasetId) == null){
			return Response.status(404).build();
		}

		return Response.ok()
					   .entity(new ArrayList<>(workers.getWorkerForDataset(datasetId).getStorage().getDataset().getTables().values()))
					   .build();
	}

	@GET
	@Path("concepts")
	public Response getConcepts(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		if(workers.getWorkerForDataset(datasetId) == null){
			return Response.status(404).build();
		}

		return Response.ok()
					   .entity(new ArrayList<>(workers.getWorkerForDataset(datasetId).getStorage().getAllConcepts()))
					   .build();
	}
}
