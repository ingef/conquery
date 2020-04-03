package com.bakdata.conquery.commands;

import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.resources.ResourceConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Produces({ExtraMimeTypes.JSON_STRING})
@Path("workers")
@RequiredArgsConstructor
public class WorkerAPI {

	@Getter(AccessLevel.NONE)
	private final Workers workers;

	@GET
	public Response listWorkers() {
		return Response.ok().entity(workers.getWorkers().keySet()).build();
	}

	@GET
	@Path("{" + ResourceConstants.DATASET + "}/" + "tables")
	public Response getTables(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		if (workers.getWorkerForDataset(datasetId) == null) {
			return Response.status(404).build();
		}

		return Response.ok()
					   .entity(new ArrayList<>(workers.getWorkerForDataset(datasetId).getStorage().getDataset().getTables().values()))
					   .build();
	}

	@GET
	@Path("{" + ResourceConstants.DATASET + "}/" + "concepts")
	public Response getConcepts(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		if (workers.getWorkerForDataset(datasetId) == null) {
			return Response.status(404).build();
		}

		return Response.ok()
					   .entity(new ArrayList<>(workers.getWorkerForDataset(datasetId).getStorage().getAllConcepts()))
					   .build();
	}

	@GET
	@Path("{" + ResourceConstants.DATASET + "}/" + "imports")
	public Response getImports(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		if (workers.getWorkerForDataset(datasetId) == null) {
			return Response.status(404).build();
		}

		return Response.ok()
					   .entity(new ArrayList<>(workers.getWorkerForDataset(datasetId).getStorage().getAllImports()))
					   .build();
	}

	@GET
	@Path("{" + ResourceConstants.DATASET + "}/" + "buckets")
	public Response getBuckets(@PathParam(ResourceConstants.DATASET) DatasetId datasetId) {
		if (workers.getWorkerForDataset(datasetId) == null) {
			return Response.status(404).build();
		}

		return Response.ok()
					   .entity(workers.getWorkerForDataset(datasetId).getStorage().getAllBuckets().stream().map(Bucket::getId).collect(Collectors.toList()))
					   .build();
	}
}
