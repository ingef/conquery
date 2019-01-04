package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.FILENAME;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;

import java.io.IOException;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilderException;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ResourceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Path("datasets/{" + DATASET + "}/form-queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
@Slf4j
public class FormResource {

	private FormProcessor processor;
	private ResourceUtil dsUtil;

	public FormResource(Namespaces namespaces) {
		this.processor = new FormProcessor();
		this.dsUtil = new ResourceUtil(namespaces);
	}

	@POST
	@Path("")
	public SQStatus post(@Auth User user, @PathParam(DATASET) DatasetId datasetId, ObjectNode jsonForm, @Context HttpServletRequest req) throws JsonProcessingException, IOException, IllegalArgumentException, UriBuilderException, InterruptedException {//@Valid com.bakdata.conquery.feforms.psm.FeForm form) {
		Dataset dataset = dsUtil.getDataset(datasetId);

//                dsUtil.getStorage(datasetId).get
		return null;
	}

	@GET
	@Path("{" + QUERY + "}")
	public SQStatus get(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws IllegalArgumentException, UriBuilderException, IOException, InterruptedException {

		Dataset dataset = dsUtil.getDataset(datasetId);
		ManagedQuery query = dsUtil.getStorage(datasetId).getMetaStorage().getQuery(queryId);

		SQStatus status = processor.get(user, dataset, query, URLBuilder.fromRequest(req));

		if (status != null) {
			return status;
		}

		throw new WebApplicationException("No form query " + queryId + " found", Status.NOT_FOUND);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public SQStatus cancel(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws IllegalArgumentException, UriBuilderException, IOException, InterruptedException {
		Dataset dataset = dsUtil.getDataset(datasetId);
		ManagedQuery query = dsUtil.getStorage(datasetId).getMetaStorage().getQuery(queryId);
		processor.cancel(user, dataset, query);
		return get(user, datasetId, queryId, req);
	}

	@GET
	@Path("download/{" + QUERY + "}/{" + FILENAME + "}")
	@Produces(AdditionalMediaTypes.CSV)
	public Response download(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId) throws JsonProcessingException, IOException {//@Valid com.bakdata.conquery.feforms.psm.FeForm form) {
		Dataset dataset = dsUtil.getDataset(datasetId);
		ManagedQuery query = dsUtil.getStorage(datasetId).getMetaStorage().getQuery(queryId);

		log.info("Querying results for {}", queryId);
		/*
		//IFormQuery fqi = processor.getQuery(user, query);
		if(fqi==null)
			throw new WebApplicationException("No form query "+queryId+" found", Status.NOT_FOUND);
		if(fqi.getCQuery().getStatus()!=StatusCode.DONE)
			throw new WebApplicationException("Form query "+queryId + " not DONE", Status.NOT_FOUND);
		
		log.info("Streaming results for {}",queryId);
		
		
		StreamingOutput out = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try {
					fqi.streamResults(output);
				} catch (Exception e) {
					throw new WebApplicationException("Failed to load result "+id.get(), e);
				}
			}
		};
		
		return Response
			.ok()
			.entity(out)
			.type(fqi.getMediaType())
			.header(HttpHeaders.CONTENT_LENGTH, fqi.getContentLength())
			.build();
		 */
		return null;
	}
}
