package com.bakdata.eva.forms.resources;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilderException;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ResourceUtil;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.managed.ManagedForm;
import com.mashape.unirest.http.exceptions.UnirestException;

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

	public FormResource(Namespaces namespaces, FormProcessor processor) {
		this.processor = processor;
		this.dsUtil = new ResourceUtil(namespaces);
	}

	@POST @Path("")
	public ExecutionStatus postQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @NotNull @Valid Form form, @Context HttpServletRequest req) throws JSONException, IOException, UnirestException, IllegalArgumentException, UriBuilderException, InterruptedException {
		authorize(user, datasetId, Ability.READ);
		
		// Check reused query
		for(ManagedExecutionId requiredQueryId : form.getUsedQueries()) {
			authorize(user, requiredQueryId, Ability.READ);
		}
		
		ManagedForm query = processor.postForm(dsUtil.getDataset(datasetId), form, user);
		
		//avoid the wait in post
		return processor.getStatus(query.getId(), URLBuilder.fromRequest(req));
	}

	@GET
	@Path("{" + QUERY + "}")
	public ExecutionStatus get(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId, @Context HttpServletRequest req) throws IllegalArgumentException, UriBuilderException, IOException, InterruptedException {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);
		ManagedForm query = processor.get(queryId);
		query.awaitDone(10, TimeUnit.SECONDS);
		return processor.getStatus(queryId, URLBuilder.fromRequest(req));
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public ExecutionStatus cancel(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId, @Context HttpServletRequest req) throws IllegalArgumentException, UriBuilderException, IOException, InterruptedException {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);
		
		Dataset dataset = dsUtil.getDataset(datasetId);
		ManagedForm query = processor.get(queryId);
		//processor.cancel(dataset, query);
		return processor.getStatus(queryId, URLBuilder.fromRequest(req));
	}

}
